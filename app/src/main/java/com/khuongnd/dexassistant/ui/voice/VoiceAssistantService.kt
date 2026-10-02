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
    private val wakeLock by lazy {
        getSystemService(PowerManager::class.java).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "DeX:Voice")
            .apply { setReferenceCounted(false) }
    }
    private val focusRequest by lazy {
        AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANT)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
            .setOnAudioFocusChangeListener({ change ->
                if (change == AudioManager.AUDIOFOCUS_LOSS || change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                    if (stateMachine.state !in setOf(VoiceState.STOPPED, VoiceState.PAUSED, VoiceState.ERROR)) {
                        cancelSession()
                        transitionSafely(VoiceEvent.PAUSE)
                        report("AUDIO_FOCUS_LOST; resume when microphone is available")
                    }
                }
            }, main).build()
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
        feedback = VoiceFeedback(this) { status ->
            preferences.edit().putString(KEY_TTS_STATUS, status).apply()
            report(lastDetail)
        }
        coordinator.start()
        coordinator.connect()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action ?: ACTION_START) {
            ACTION_STOP -> {
                cancelSession()
                transitionSafely(VoiceEvent.STOP)
                publishHealth(VoiceState.STOPPED, "Stopped by user")
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_PAUSE -> {
                cancelSession()
                if (stateMachine.state != VoiceState.STOPPED) transitionSafely(VoiceEvent.PAUSE)
            }
            ACTION_RESUME -> if (stateMachine.state == VoiceState.PAUSED) transitionSafely(VoiceEvent.RESUME)
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
        report("Loading offline Hey DeX wake model")
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
                    main.post { if (sessions.isCurrent(id)) report("Listening for Hey DeX offline") }
                    val samples = ShortArray(320)
                    while (sessions.isCurrent(id)) {
                        val count = input.read(samples, 0, samples.size, AudioRecord.READ_BLOCKING)
                        if (!sessions.isCurrent(id)) break
                        check(count > 0) { "Microphone read failed" }
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
                        startRecognition(id)
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
        if (audioManager.requestAudioFocus(focusRequest) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            transitionSafely(VoiceEvent.STT_STARTED)
            finishWithFeedback(id, "Micro đang bận. Hãy gọi lại sau.", "AUDIO_FOCUS_DENIED")
            return
        }
        focusHeld = true
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
        override fun onReadyForSpeech(params: Bundle?) {
            if (isRecognizing(id)) { feedback.readyTone(); report("Ready for Vietnamese command") }
        }
        override fun onBeginningOfSpeech() = Unit
        override fun onRmsChanged(rmsdB: Float) = Unit
        override fun onBufferReceived(buffer: ByteArray?) = Unit // Raw audio is never stored.
        override fun onEndOfSpeech() = Unit
        override fun onPartialResults(partialResults: Bundle?) = Unit
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
            destroyRecognizer()
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
                finishWithFeedback(id, spokenResult(result.status), result.status)
            }
            returnedId = requestId
            if (!completedSynchronously && sessions.isCurrent(id)) commandId = requestId
        }
    }

    private fun spokenResult(status: String): String = when (status) {
        "SUCCESS", "VERIFIED", "COMPLETED" -> "Đã thực hiện."
        "DISPATCHED" -> "Đã gửi yêu cầu."
        "PLANNED" -> "Đã lập kế hoạch, chưa thực hiện trên máy."
        "UNSUPPORTED", "LOCAL_ONLY_UNSUPPORTED" -> "Lệnh này chưa được hỗ trợ."
        "AI_UNAVAILABLE", "AI_TIMEOUT", "AI_QUOTA_EXHAUSTED", "SEND_FAILED" -> "Khó quá, không làm được."
        else -> "Không thực hiện được. Hãy thử lại."
    }

    private fun finishWithFeedback(id: Long, message: String, code: String) {
        if (!sessions.isCurrent(id)) return
        destroyRecognizer()
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
        feedback.speak(message, "voice-$id", completed)
    }

    private fun destroyRecognizer() {
        watchdog?.let(main::removeCallbacks)
        watchdog = null
        val old = recognizer
        recognizer = null
        recognizerEpoch = null
        runCatching { old?.cancel(); old?.destroy() }
    }

    private fun cancelSession() {
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
        if (focusHeld) audioManager.abandonAudioFocusRequest(focusRequest)
        focusHeld = false
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
