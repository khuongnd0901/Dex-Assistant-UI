package com.khuongnd.dexassistant.ui.voice

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener

/** Vietnamese canned feedback only: never speaks the raw command or credentials. */
class VoiceFeedback(context: Context, private val capabilityChanged: (String) -> Unit = {}) : AutoCloseable {
    private val main = Handler(Looper.getMainLooper())
    private val tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 60)
    private var ready = false
    private var initialized = false
    private var closed = false
    private var pending: (() -> Unit)? = null
    private var completion: (() -> Unit)? = null
    private var utterance: String? = null
    var voiceDescription = "TTS_INITIALIZING"
        private set
    private var engine: TextToSpeech? = null
    private var selectedNetwork = false
    private fun describe(value: String) { voiceDescription = value; capabilityChanged(value) }
    private val initTimeout = Runnable {
        if (!initialized) {
            initialized = true
            describe("TTS_INIT_TIMEOUT")
            pending?.also { pending = null; it() }
        }
    }

    init {
        engine = TextToSpeech(context.applicationContext) { status -> main.post {
            if (closed || initialized) return@post
            initialized = true
            main.removeCallbacks(initTimeout)
            val tts = engine
            if (status == TextToSpeech.SUCCESS && tts != null) {
                val voices = tts.voices.orEmpty().filter {
                    it.locale.language == "vi" && it.locale.country == "VN" &&
                        "notInstalled" !in it.features.orEmpty()
                }.sortedWith(compareBy({ it.isNetworkConnectionRequired }, { it.name }))
                val voice = voices.firstOrNull()
                ready = voice != null && tts.setVoice(voice) == TextToSpeech.SUCCESS
                selectedNetwork = voice?.isNetworkConnectionRequired == true
                describe(if (ready) {
                    if (voice!!.isNetworkConnectionRequired) "TTS_VI_NETWORK_READY" else "TTS_VI_OFFLINE_READY"
                } else "TTS_VI_UNAVAILABLE")
            } else describe("TTS_INIT_FAILED")
            pending?.also { pending = null; it() }
        } }
        engine?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit
            override fun onDone(utteranceId: String?) { finish(utteranceId, false) }
            @Deprecated("Android callback") override fun onError(utteranceId: String?) { finish(utteranceId, true) }
            override fun onError(utteranceId: String?, errorCode: Int) { finish(utteranceId, true) }
        })
        main.postDelayed(initTimeout, 10_000)
    }

    private var cue: AudioTrack? = null
    private var cueRelease: Runnable? = null
    var readyCueDescription = "READY_CUE_IDLE"
        private set

    /** No audio focus request: the recognizer already owns its listening session. */
    fun readyTone() {
        if (closed) return
        cancelReadyTone()
        var track: AudioTrack? = null
        try {
            val samples = ReadyCueSignal.samples()
            track = AudioTrack.Builder()
                .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                .setAudioFormat(AudioFormat.Builder().setSampleRate(ReadyCueSignal.SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).setEncoding(AudioFormat.ENCODING_PCM_16BIT).build())
                .setTransferMode(AudioTrack.MODE_STATIC).setBufferSizeInBytes(samples.size * 2).build()
            check(track.write(samples, 0, samples.size) == samples.size)
            cue = track
            track.play()
            readyCueDescription = "READY_CUE_MEDIA_DISPATCHED"
            android.util.Log.i("DexVoice", readyCueDescription)
            val current = track
            val release = Runnable {
                if (cue === current) {
                    readyCueDescription = "READY_CUE_MEDIA_FINISHED_route_${current.routedDevice?.type ?: -1}"
                    android.util.Log.i("DexVoice", readyCueDescription)
                    cancelReadyTone()
                }
            }
            cueRelease = release
            main.postDelayed(release, ReadyCueSignal.DURATION_MS + 250L)
        } catch (failure: RuntimeException) {
            if (cue === track) cue = null
            runCatching { track?.release() }
            readyCueDescription = "READY_CUE_FAILED_${failure.javaClass.simpleName}"
            android.util.Log.w("DexVoice", readyCueDescription)
        }
    }

    fun cancelReadyTone() {
        cueRelease?.let(main::removeCallbacks)
        cueRelease = null
        val old = cue
        cue = null
        runCatching { old?.stop() }
        runCatching { old?.release() }
    }

    fun speak(text: String, id: String, done: () -> Unit) {
        if (closed) { done(); return }
        if (!initialized) { pending = { speak(text, id, done) }; return }
        if (!ready) {
            tone.startTone(ToneGenerator.TONE_PROP_NACK, 120)
            done()
            return
        }
        utterance = id
        completion = done
        if (engine?.speak(text, TextToSpeech.QUEUE_FLUSH, null, id) != TextToSpeech.SUCCESS) {
            finish(id, true)
        }
    }

    private fun finish(id: String?, failed: Boolean) = main.post {
        if (id != null && utterance == id) {
            utterance = null
            describe(if (failed) "TTS_PLAYBACK_FAILED" else if (selectedNetwork) "TTS_VI_NETWORK_SYNTHESIZED" else "TTS_VI_OFFLINE_SYNTHESIZED")
            if (failed) tone.startTone(ToneGenerator.TONE_PROP_NACK, 120)
            completion?.also { completion = null; it() }
        }
    }

    fun timeout() { describe("TTS_PLAYBACK_TIMEOUT"); cancel() }

    fun cancel() {
        cancelReadyTone()
        pending = null
        completion = null
        utterance = null
        engine?.stop()
    }

    override fun close() {
        closed = true
        main.removeCallbacks(initTimeout)
        cancel()
        engine?.shutdown()
        engine = null
        tone.release()
    }
}
