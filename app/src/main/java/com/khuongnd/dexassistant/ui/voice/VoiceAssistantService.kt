package com.khuongnd.dexassistant.ui.voice

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.khuongnd.dexassistant.ui.MainActivity

class VoiceAssistantService : Service() {
    private val stateMachine = VoiceStateMachine()
    private val preferences by lazy { getSharedPreferences(HEALTH_PREFERENCES, MODE_PRIVATE) }
    @Volatile private var probe: AudioRecord? = null
    private val main = Handler(Looper.getMainLooper())
    private val probeCoordinator = ProbeCoordinator()
    private val probeLock = Any()

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action ?: ACTION_START) {
            ACTION_STOP -> {
                stopProbe()
                stateMachine.accept(VoiceEvent.STOP)
                publishHealth(VoiceState.STOPPED, "Stopped by user")
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_PAUSE -> { stopProbe(); transitionSafely(VoiceEvent.PAUSE) }
            ACTION_RESUME -> transitionSafely(VoiceEvent.RESUME)
            else -> transitionSafely(VoiceEvent.START)
        }
        val detail = if (stateMachine.state == VoiceState.PAUSED) "Paused by user" else "Opening microphone probe"
        publishHealth(stateMachine.state, detail)
        startInForeground(notification(stateMachine.state, detail))
        if (stateMachine.state == VoiceState.LISTENING) runMicrophoneProbe()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopProbe()
        super.onDestroy()
    }

    private fun runMicrophoneProbe() {
        val generation = probeCoordinator.begin() ?: return
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            probeCoordinator.complete(generation)
            transitionSafely(VoiceEvent.FAILURE)
            publishHealth(VoiceState.ERROR, "Microphone probe failed: RECORD_AUDIO_DENIED")
            updateNotification(VoiceState.ERROR, "Microphone probe failed: RECORD_AUDIO_DENIED")
            return
        }
        Thread {
            var recorder: AudioRecord? = null
            val result = runCatching {
                val sampleRate = 16_000
                val minimum = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT)
                check(minimum > 0) { "Unsupported audio format" }
                val audio = AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, sampleRate,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
                    minimum.coerceAtLeast(sampleRate))
                recorder = audio
                check(audio.state == AudioRecord.STATE_INITIALIZED) { "AudioRecord initialization failed" }
                synchronized(probeLock) {
                    check(probeCoordinator.isCurrent(generation)) { "Probe cancelled" }
                    probe = audio
                    audio.startRecording()
                }
                check(audio.recordingState == AudioRecord.RECORDSTATE_RECORDING) { "AudioRecord start failed" }
                check(audio.read(ShortArray(320), 0, 320, AudioRecord.READ_BLOCKING) >= 0) { "AudioRecord read failed" }
            }
            runCatching { recorder?.stop() }
            recorder?.release()
            main.post {
                if (!probeCoordinator.complete(generation)) return@post
                probe = null
                if (result.isSuccess) {
                    publishHealth(VoiceState.LISTENING, "Microphone probe passed; wake-word runtime pending")
                    updateNotification(VoiceState.LISTENING, "Microphone probe passed; wake-word runtime pending")
                } else {
                    transitionSafely(VoiceEvent.FAILURE)
                    val failure = "Microphone probe failed: ${result.exceptionOrNull()?.javaClass?.simpleName}"
                    publishHealth(VoiceState.ERROR, failure)
                    updateNotification(VoiceState.ERROR, failure)
                }
            }
        }.start()
    }

    private fun stopProbe() {
        synchronized(probeLock) {
            probeCoordinator.cancel()
            val recorder = probe
            probe = null
            // The worker owns release; stop only interrupts its read.
            runCatching { recorder?.stop() }
        }
    }

    private fun transitionSafely(event: VoiceEvent) {
        runCatching { stateMachine.accept(event) }.onFailure { stateMachine.accept(VoiceEvent.FAILURE) }
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
        const val KEY_ASSISTANT_READINESS = "assistantReadiness"
        private const val CHANNEL_ID = "dex_voice"
        private const val NOTIFICATION_ID = 1001
    }
}
