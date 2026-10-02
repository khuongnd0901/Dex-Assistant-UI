package com.khuongnd.dexassistant.ui.voice

import android.content.Context
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

    fun readyTone() { if (!closed) tone.startTone(ToneGenerator.TONE_PROP_BEEP, 80) }

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
