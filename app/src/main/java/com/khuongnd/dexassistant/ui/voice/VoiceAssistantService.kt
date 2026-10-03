package com.khuongnd.dexassistant.ui.voice

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.speech.RecognitionListener
import android.speech.SpeechRecognizer
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.khuongnd.dexassistant.ui.AssistantController
import com.khuongnd.dexassistant.ui.MainActivity
import java.util.concurrent.Executors
import com.khuongnd.dexassistant.ui.screen.DexAccessibilityService

/** Microphone ownership is sequential: wake recorder -> STT service -> feedback -> wake. */
class VoiceAssistantService : Service() {
    private val stateMachine = VoiceStateMachine()
    private val sessions = VoiceSessionGuard()
    private val preferences by lazy { getSharedPreferences(HEALTH_PREFERENCES, MODE_PRIVATE) }
    private val main = Handler(Looper.getMainLooper())
    private val wakeWorker = Executors.newSingleThreadExecutor()
    private val microphoneLock = Any()
    private var recorder: AudioRecord? = null
    private var recognizer: SpeechRecognizer? = null
    private var recognizerEpoch: Long? = null
    private var watchdog: Runnable? = null
    private var feedbackWatchdog: Runnable? = null
    private var commandId: String? = null
    private var lastDetail = "Initializing voice pipeline"
    private lateinit var feedback: VoiceFeedback
    private val coordinator by lazy { AssistantController.shared(this) }
    private val audioManager by lazy { getSystemService(AudioManager::class.java) }
    private var focusHeld = false
    private var diagnosticOnly = false
    private var diagnosticRestore = VoiceState.STOPPED
    private var lastMicUpdate = 0L
    private val wakeLock by lazy {
        getSystemService(PowerManager::class.java).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "DeX:Voice")
            .apply { setReferenceCounted(false) }
    }
    private var focusRequest: AudioFocusRequest? = null
    private var focusGeneration = 0L
    private fun newFocusRequest(generation: Long): AudioFocusRequest =
        AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANT)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
            .setOnAudioFocusChangeListener({ change ->
                if (change == AudioManager.AUDIOFOCUS_LOSS || change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                    if (VoiceFocusPolicy.pauseForLoss(focusHeld, stateMachine.state, generation, focusGeneration)) {
                        cancelSession()
                        transitionSafely(VoiceEvent.PAUSE)
                        report("PAUSED_EXTERNAL_AUDIO_FOCUS; nhấn Tiếp tục để nghe lại")
                    }
                }
            }, main).build()

    override fun onCreate() {
        super.onCreate()
        active = this
        createChannel()
        feedback = VoiceFeedback(this) { status ->
            preferences.edit().putString(KEY_TTS_STATUS, status).apply()
            report(lastDetail)
        }
        coordinator.start()
        coordinator.connect()
    }

    fun currentVoiceState(): VoiceState = stateMachine.state
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_AUTO_START
        if (action == ACTION_AUTO_START && !VoiceAutoStart.allowed(this)) {
            if (stateMachine.state != VoiceState.STOPPED) return START_STICKY
            startInForeground(notification(VoiceState.STOPPED, "Tự nghe bị tạm dừng theo lựa chọn của người dùng"))
            stopForeground(STOP_FOREGROUND_REMOVE); stopSelf(); return START_NOT_STICKY
        }
        if (action == ACTION_START || action == ACTION_RESUME) VoiceAutoStart.userResume(this)
        if (intent?.action == ACTION_DIAGNOSTIC) {
            val previous = stateMachine.state
            cancelSession(); VoiceLiveState.begin(true); diagnosticOnly = true
            diagnosticRestore = if (previous == VoiceState.LISTENING) VoiceState.LISTENING else if (previous == VoiceState.STOPPED) VoiceState.STOPPED else VoiceState.PAUSED
            transitionSafely(VoiceEvent.STOP); transitionSafely(VoiceEvent.START); transitionSafely(VoiceEvent.WAKE)
            startInForeground(notification(stateMachine.state, "Nghe thử — không thực hiện lệnh"))
            val id = sessions.begin()
            // Queue behind the prior wake task so stop AND release precede external STT capture.
            wakeWorker.execute { main.post { if (sessions.isCurrent(id)) startRecognition(id) } }
            return START_NOT_STICKY
        }
        when (action) {
            ACTION_STOP -> {
                VoiceAutoStart.suppress(this, "USER_STOPPED")
                VoiceLiveState.clear()
                cancelSession()
                transitionSafely(VoiceEvent.STOP)
                publishHealth(VoiceState.STOPPED, "Stopped by user")
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_PAUSE -> {
                VoiceAutoStart.suppress(this, "USER_PAUSED")
                cancelSession()
                if (stateMachine.state != VoiceState.STOPPED) transitionSafely(VoiceEvent.PAUSE)
            }
            ACTION_AUTO_START -> if (stateMachine.state == VoiceState.PAUSED) transitionSafely(VoiceEvent.RESUME) else transitionSafely(VoiceEvent.START)
            ACTION_RESUME -> if (stateMachine.state == VoiceState.PAUSED) transitionSafely(VoiceEvent.RESUME)
                else if (stateMachine.state in setOf(VoiceState.STOPPED, VoiceState.ERROR)) transitionSafely(VoiceEvent.START)
            else -> transitionSafely(VoiceEvent.START)
        }
        val detail = if (stateMachine.state == VoiceState.PAUSED) "Paused by user" else "Voice pipeline active"
        publishHealth(stateMachine.state, detail)
        startInForeground(notification(stateMachine.state, detail))
        if (stateMachine.state == VoiceState.LISTENING && activeWakeEpoch == null) startWake()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
    override fun onDestroy() {
        active = null
        cancelSession()
        wakeWorker.shutdownNow()
        feedback.close()
        transitionSafely(VoiceEvent.STOP)
        publishHealth(VoiceState.STOPPED, "Voice service destroyed")
        super.onDestroy()
    }

    private var activeWakeEpoch: Long? = null

    @SuppressLint("MissingPermission", "WakelockTimeout") // Actual permission is checked before constructing AudioRecord.
    private fun startWake() {
        if (stateMachine.state != VoiceState.LISTENING) return
        if (!wakeLock.isHeld) wakeLock.acquire() // Always-on opt-in; released on pause, stop or runtime failure.
        val id = sessions.begin()
        activeWakeEpoch = id
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            activeWakeEpoch = null
            transitionSafely(VoiceEvent.FAILURE)
            if (wakeLock.isHeld) wakeLock.release()
            report("WAKE_PERMISSION_DENIED")
            return
        }
        report("Loading offline Hey Hey wake model")
        wakeWorker.execute {
            if (!sessions.isCurrent(id)) return@execute
            var detected = false
            var audio: AudioRecord? = null
            val result = runCatching {
                SherpaWakeEngine(assets).use { engine ->
                    if (!sessions.isCurrent(id)) return@use
                    val minimum = AudioRecord.getMinBufferSize(16_000, AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT)
                    check(minimum > 0) { "Unsupported microphone format" }
                    val input = AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, 16_000,
                        AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, minimum.coerceAtLeast(3200))
                    audio = input
                    check(input.state == AudioRecord.STATE_INITIALIZED) { "Microphone initialization failed" }
                    synchronized(microphoneLock) {
                        if (!sessions.isCurrent(id)) return@use
                        recorder = input
                        input.startRecording()
                    }
                    check(input.recordingState == AudioRecord.RECORDSTATE_RECORDING) { "Microphone start failed" }
                    main.post { if (sessions.isCurrent(id)) report("Listening for Hey Hey offline") }
                    val samples = ShortArray(320)
                    while (sessions.isCurrent(id)) {
                        val count = input.read(samples, 0, samples.size, AudioRecord.READ_BLOCKING)
                        if (!sessions.isCurrent(id)) break
                        check(count > 0) { "Microphone read failed" }
                        val now = SystemClock.elapsedRealtime()
                        if (now - lastMicUpdate >= 1000) {
                            lastMicUpdate = now
                            val rms = kotlin.math.sqrt(samples.take(count).sumOf { it.toDouble() * it } / count) / 32768.0
                            val device = input.routedDevice
                            val route = when (device?.type) {
                                android.media.AudioDeviceInfo.TYPE_BUILTIN_MIC -> "Micro điện thoại"
                                android.media.AudioDeviceInfo.TYPE_USB_DEVICE, android.media.AudioDeviceInfo.TYPE_USB_HEADSET -> "Micro USB"
                                android.media.AudioDeviceInfo.TYPE_BLUETOOTH_SCO, android.media.AudioDeviceInfo.TYPE_BLE_HEADSET -> "Micro Bluetooth"
                                null -> "Micro chưa xác minh"
                                else -> "Micro hệ thống"
                            }
                            val blocked = when (input.activeRecordingConfiguration?.isClientSilenced) { true -> "âm thanh bị hệ thống chặn"; false -> "đang nhận âm thanh"; null -> "chưa xác minh âm thanh" }
                            VoiceLiveState.microphone("$route · $blocked · RMS %.3f".format(java.util.Locale.ROOT, rms))
                        }
                        if (engine.accept(samples, count)) { detected = true; break }
                    }
                }
            }
            // No STT callback is posted until microphone stop AND release have completed.
            synchronized(microphoneLock) {
                runCatching { audio?.stop() }
                audio?.release()
                if (recorder === audio) recorder = null
            }
            main.post {
                if (!sessions.isCurrent(id)) return@post
                activeWakeEpoch = null
                when {
                    detected -> {
                        transitionSafely(VoiceEvent.WAKE)
                        report("Wake detected; opening Vietnamese STT")
                        val screen = DexAccessibilityService.active
                        if (screen == null) startRecognition(id) else screen.prepare { status ->
                            if (sessions.isCurrent(id)) {
                                report("Numbered screen: $status; opening Vietnamese STT")
                                startRecognition(id)
                            }
                        }
                    }
                    result.isFailure -> {
                        transitionSafely(VoiceEvent.FAILURE)
                        if (wakeLock.isHeld) wakeLock.release()
                        report("WAKE_RUNTIME_FAILED:${result.exceptionOrNull()?.javaClass?.simpleName}")
                    }
                }
            }
        }
    }

    private fun startRecognition(id: Long) {
        if (!sessions.isCurrent(id)) return
        val component = CommandRecognizer.component(this)
        if (component == null) {
            // Maintain a complete lifecycle even when the device has no command STT service.
            transitionSafely(VoiceEvent.STT_STARTED)
            finishWithFeedback(id, "Không có dịch vụ nhận dạng tiếng Việt.", "STT_SERVICE_UNAVAILABLE")
            return
        }
        // The external recognizer owns its capture/focus. Abandon app feedback focus before handoff.
        abandonFocus()
        if (!diagnosticOnly) VoiceLiveState.begin(false)
        VoiceLiveState.microphone("STT: ${component.packageName} · route do dịch vụ nhận dạng quản lý")
        transitionSafely(VoiceEvent.STT_STARTED)
        report("STT vi-VN via ${component.packageName}; online permitted")
        runCatching {
            recognizer = SpeechRecognizer.createSpeechRecognizer(this, component)
            recognizerEpoch = id
            recognizer!!.setRecognitionListener(CommandListener(id))
            watchdog = Runnable {
                if (isRecognizing(id)) {
                    destroyRecognizer()
                    finishWithFeedback(id, "Hết thời gian nghe. Hãy gọi lại.", "STT_TIMEOUT")
                }
            }.also { main.postDelayed(it, 30_000) }
            recognizer!!.startListening(CommandRecognizer.intent())
        }.onFailure {
            destroyRecognizer()
            finishWithFeedback(id, "Không thể nhận giọng nói. Hãy gọi lại.", "STT_START_FAILED")
        }
    }

    private fun isRecognizing(id: Long) = sessions.isCurrent(id) && recognizerEpoch == id && recognizer != null

    private inner class CommandListener(private val id: Long) : RecognitionListener {
        private var announcedReady = false
        override fun onReadyForSpeech(params: Bundle?) {
            if (isRecognizing(id) && !announcedReady) {
                announcedReady = true
                VoiceLiveState.ready(true)
                feedback.readyTone()
                report("Ready for Vietnamese command; ${feedback.readyCueDescription}")
            }
        }
        override fun onBeginningOfSpeech() = Unit
        override fun onRmsChanged(rmsdB: Float) {
            if (isRecognizing(id) && SystemClock.elapsedRealtime() - lastMicUpdate >= 500) {
                lastMicUpdate = SystemClock.elapsedRealtime()
                VoiceLiveState.microphone("STT RMS %.1f dB · micro do dịch vụ nhận dạng quản lý".format(java.util.Locale.ROOT, rmsdB))
            }
        }
        override fun onBufferReceived(buffer: ByteArray?) = Unit // Raw audio is never stored.
        override fun onEndOfSpeech() = Unit
        override fun onPartialResults(partialResults: Bundle?) {
            if (isRecognizing(id)) VoiceLiveState.text(partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty(), false)
        }
        override fun onEvent(eventType: Int, params: Bundle?) = Unit
        override fun onError(error: Int) {
            if (!isRecognizing(id)) return
            destroyRecognizer()
            val code = CommandRecognizer.errorCode(error)
            val message = if (code == "STT_NETWORK") "Không có kết nối nhận giọng nói. Hãy gọi lại sau."
                else "Chưa nghe rõ. Hãy gọi lại."
            finishWithFeedback(id, message, code)
        }
        override fun onResults(results: Bundle?) {
            if (!isRecognizing(id) || !sessions.consumeFinal(id)) return
            val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.trim().orEmpty()
            VoiceLiveState.text(text, true)
            destroyRecognizer()
            if (diagnosticOnly) {
                finishDiagnostic(id, "COMPLETE")
                return
            }
            if (text.isBlank()) { finishWithFeedback(id, "Chưa nghe rõ. Hãy gọi lại.", "STT_EMPTY"); return }
            android.util.Log.i("DexMetric", "stt.final sessionId=$id elapsedNanos=${SystemClock.elapsedRealtimeNanos()}")
            transitionSafely(VoiceEvent.FINAL_TEXT)
            report("Routing final Vietnamese command")
            var returnedId: String? = null
            var completedSynchronously = false
            val requestId = coordinator.dispatchVoice(text, progress = { request, stage ->
                if (sessions.isCurrent(id) && (commandId == request || returnedId == null)) {
                    if (stage == "SENDING" && stateMachine.state == VoiceState.ROUTING) transitionSafely(VoiceEvent.BACKEND_SENT)
                    if (stage == "EXECUTING" && stateMachine.state in setOf(VoiceState.ROUTING, VoiceState.SENDING)) {
                        transitionSafely(VoiceEvent.PLAN_RECEIVED)
                    }
                    report("$stage requestId=$request")
                }
            }) { result ->
                if (!sessions.isCurrent(id)) return@dispatchVoice
                completedSynchronously = true
                if (commandId == result.requestId || returnedId == null) commandId = null
                if (result.status == "SCREEN_CONTINUE") {
                    transitionSafely(VoiceEvent.COMMAND_FINISHED)
                    transitionSafely(VoiceEvent.WAKE)
                    startRecognition(sessions.begin())
                } else finishWithFeedback(id, spokenResult(result.status), result.status)
            }
            returnedId = requestId
            if (!completedSynchronously && sessions.isCurrent(id)) commandId = requestId
        }
    }

    private fun finishDiagnostic(id: Long, outcome: String) {
        if (!sessions.isCurrent(id)) return
        diagnosticOnly = false
        when (diagnosticRestore) {
            VoiceState.LISTENING -> { transitionSafely(VoiceEvent.PAUSE); transitionSafely(VoiceEvent.RESUME); report("DIAGNOSTIC_$outcome; không thực hiện lệnh"); startWake() }
            VoiceState.STOPPED -> { transitionSafely(VoiceEvent.STOP); report("DIAGNOSTIC_$outcome; không thực hiện lệnh"); stopForeground(STOP_FOREGROUND_REMOVE); stopSelf() }
            else -> { transitionSafely(VoiceEvent.PAUSE); report("DIAGNOSTIC_$outcome; không thực hiện lệnh") }
        }
    }
    private fun spokenResult(status: String): String = when (status) {
        "SUCCESS", "VERIFIED", "COMPLETED" -> "Đã thực hiện."
        "DISPATCHED" -> "Đã gửi yêu cầu."
        "CANCELLED" -> "Đã hủy."
        "ACCESSIBILITY_PERMISSION_REQUIRED" -> "Hãy bật dịch vụ Trợ năng DeX trong cài đặt."
        "SCREEN_SESSION_EXPIRED", "SCREEN_CHANGED" -> "Màn hình đã thay đổi. Hãy gọi lại."
        "INVALID_SCREEN_NUMBER", "INVALID_SCREEN_CELL", "INVALID_SCREEN_COMMAND" -> "Số hoặc lệnh chưa hợp lệ. Hãy gọi lại."
        "PLANNED" -> "Đã lập kế hoạch, chưa thực hiện trên máy."
        "UNSUPPORTED", "LOCAL_ONLY_UNSUPPORTED" -> "Lệnh này chưa được hỗ trợ."
        "AI_UNAVAILABLE", "AI_TIMEOUT", "AI_QUOTA_EXHAUSTED", "SEND_FAILED" -> "Khó quá, không làm được."
        else -> "Không thực hiện được. Hãy thử lại."
    }

    private fun finishWithFeedback(id: Long, message: String, code: String) {
        if (!sessions.isCurrent(id)) return
        DexAccessibilityService.active?.clear()
        destroyRecognizer()
        if (diagnosticOnly) { finishDiagnostic(id, code); return }
        transitionSafely(VoiceEvent.SPEAK)
        report("$code; ${feedback.voiceDescription}")
        val completed = {
            if (sessions.isCurrent(id) && stateMachine.state == VoiceState.SPEAKING) {
                feedbackWatchdog?.let(main::removeCallbacks)
                feedbackWatchdog = null
                abandonFocus()
                transitionSafely(VoiceEvent.FEEDBACK_FINISHED)
                startWake()
            }
        }
        feedbackWatchdog = Runnable {
            feedback.timeout()
            completed()
        }.also { main.postDelayed(it, 25_000) }
        val request = newFocusRequest(++focusGeneration)
        focusRequest = request
        focusHeld = audioManager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        if (focusHeld) feedback.speak(message, "voice-$id", completed) else completed()
    }

    private fun destroyRecognizer() {
        VoiceLiveState.ready(false)
        feedback.cancelReadyTone()
        watchdog?.let(main::removeCallbacks)
        watchdog = null
        val old = recognizer
        recognizer = null
        recognizerEpoch = null
        runCatching { old?.cancel(); old?.destroy() }
    }

    private fun cancelSession() {
        diagnosticOnly = false
        DexAccessibilityService.active?.clear()
        sessions.cancel()
        if (wakeLock.isHeld) wakeLock.release()
        activeWakeEpoch = null
        synchronized(microphoneLock) { runCatching { recorder?.stop() } }
        destroyRecognizer()
        commandId?.let(coordinator::cancelVoice)
        commandId = null
        feedbackWatchdog?.let(main::removeCallbacks)
        feedbackWatchdog = null
        if (::feedback.isInitialized) feedback.cancel()
        abandonFocus()
    }

    private fun abandonFocus() {
        val held = focusHeld; val request = focusRequest
        focusHeld = false; focusRequest = null; focusGeneration++
        if (held && request != null) audioManager.abandonAudioFocusRequest(request)
    }

    private fun transitionSafely(event: VoiceEvent) {
        runCatching { stateMachine.accept(event) }.onFailure { stateMachine.accept(VoiceEvent.FAILURE) }
    }

    private fun report(detail: String) {
        lastDetail = detail
        val healthDetail = if (::feedback.isInitialized) "$detail; ${feedback.voiceDescription}" else detail
        publishHealth(stateMachine.state, healthDetail)
        updateNotification(stateMachine.state, healthDetail)
    }

    private fun startInForeground(notification: Notification) {
        val type = if (Build.VERSION.SDK_INT >= 30) ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE else 0
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, type)
    }

    private fun updateNotification(state: VoiceState, detail: String) {
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification(state, detail))
    }

    private fun notification(state: VoiceState, detail: String): Notification {
        val open = PendingIntent.getActivity(this, 1, Intent(this, MainActivity::class.java), immutableFlags())
        fun action(name: String, request: Int) = PendingIntent.getService(
            this, request, Intent(this, VoiceAssistantService::class.java).setAction(name), immutableFlags())
        val paused = state == VoiceState.PAUSED
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("DeX Assistant · ${state.name}")
            .setContentText(detail)
            .setOngoing(state != VoiceState.STOPPED)
            .setContentIntent(open)
            .addAction(0, if (paused) "Tiếp tục" else "Tạm dừng", action(if (paused) ACTION_RESUME else ACTION_PAUSE, 2))
            .addAction(0, "Dừng", action(ACTION_STOP, 3))
            .addAction(0, "Mở quản trị", open)
            .build()
    }

    private fun publishHealth(state: VoiceState, detail: String) {
        preferences.edit().putString(KEY_STATE, state.name).putString(KEY_DETAIL, detail)
            .putLong(KEY_UPDATED_AT, System.currentTimeMillis()).apply()
    }

    private fun createChannel() {
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Voice assistant", NotificationManager.IMPORTANCE_LOW),
        )
    }

    private fun immutableFlags() = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

    object WakeWordAssets {
        // Exact names expected from the official GigaSpeech 3.3M 2024-01-01 archive.
        val requiredFiles = listOf("kws/tokens.txt", "kws/encoder-epoch-12-avg-2-chunk-16-left-64.int8.onnx",
            "kws/decoder-epoch-12-avg-2-chunk-16-left-64.int8.onnx", "kws/joiner-epoch-12-avg-2-chunk-16-left-64.int8.onnx")
    }

    companion object {
        @Volatile var active: VoiceAssistantService? = null
            private set
        const val ACTION_AUTO_START = "com.khuongnd.dexassistant.ui.action.AUTO_START_VOICE"
        const val ACTION_DIAGNOSTIC = "com.khuongnd.dexassistant.ui.action.DIAGNOSTIC_LISTEN"
        const val ACTION_START = "com.khuongnd.dexassistant.ui.action.START_VOICE_SERVICE"
        const val ACTION_STOP = "com.khuongnd.dexassistant.ui.action.STOP_VOICE_SERVICE"
        const val ACTION_PAUSE = "com.khuongnd.dexassistant.ui.action.PAUSE_VOICE_SERVICE"
        const val ACTION_RESUME = "com.khuongnd.dexassistant.ui.action.RESUME_VOICE_SERVICE"
        const val HEALTH_PREFERENCES = "voice_health"
        const val KEY_STATE = "state"
        const val KEY_DETAIL = "detail"
        const val KEY_UPDATED_AT = "updatedAt"
        const val KEY_TTS_STATUS = "ttsStatus"
        const val KEY_ASSISTANT_READINESS = "assistantReadiness"
        private const val CHANNEL_ID = "dex_voice"
        private const val NOTIFICATION_ID = 1001
    }
}
