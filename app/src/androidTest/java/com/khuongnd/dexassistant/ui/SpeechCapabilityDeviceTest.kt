package com.khuongnd.dexassistant.ui

import android.content.Intent
import android.os.Build
import android.speech.RecognitionSupport
import android.speech.RecognitionSupportCallback
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/** Read-only inventory: no recording, synthesis, generic recognizer or model download. */
class SpeechCapabilityDeviceTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val timeoutSeconds = 10L

    @Test fun reportVietnameseOfflineCapabilities() {
        val stt = probeStt()
        val tts = probeTts()
        val report = "api=${Build.VERSION.SDK_INT}; locale=vi-VN; stt=$stt; tts=$tts; " +
            "recognitionExecuted=false; synthesisExecuted=false"
        Log.i("DexSpeechCapability", report)
        val results = android.os.Bundle().apply { putString("speechCapability", report) }
        instrumentation.sendStatus(0, results)
        // Availability is a diagnostic outcome, not a required device configuration.
        assertTrue(stt.isNotBlank() && tts.isNotBlank())
    }

    private fun probeStt(): String {
        if (Build.VERSION.SDK_INT < 31) return "UNKNOWN(on-device API requires 31)"
        var recognizer: SpeechRecognizer? = null
        val result = AtomicReference("UNKNOWN(support callback timeout)")
        val completed = CountDownLatch(1)
        try {
            instrumentation.runOnMainSync {
                try {
                    if (!SpeechRecognizer.isOnDeviceRecognitionAvailable(context)) {
                        result.set("UNAVAILABLE(no explicit on-device service)")
                        completed.countDown()
                    } else if (Build.VERSION.SDK_INT < 33) {
                        result.set("UNKNOWN(service present; language support API requires 33)")
                        completed.countDown()
                    } else {
                        recognizer = SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
                        val request = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "vi-VN")
                            .putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                        recognizer!!.checkRecognitionSupport(request, context.mainExecutor,
                            object : RecognitionSupportCallback {
                                override fun onSupportResult(support: RecognitionSupport) {
                                    val installed = support.installedOnDeviceLanguages
                                    val pending = support.pendingOnDeviceLanguages
                                    val downloadable = support.supportedOnDeviceLanguages
                                    val state = if (installed.any { it.equals("vi-VN", true) })
                                        "AVAILABLE(engine reports installed exact locale; recognition untested)"
                                    else "UNAVAILABLE(exact locale absent from installed inventory)"
                                    result.set("$state installed=$installed pending=$pending downloadable=$downloadable")
                                    completed.countDown()
                                }
                                override fun onError(error: Int) {
                                    val state = when (error) {
                                        SpeechRecognizer.ERROR_CANNOT_CHECK_SUPPORT -> "UNKNOWN"
                                        SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED,
                                        SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> "UNAVAILABLE"
                                        else -> "ERROR"
                                    }
                                    result.set("$state(support error=$error)")
                                    completed.countDown()
                                }
                            })
                    }
                } catch (failure: RuntimeException) {
                    result.set("ERROR(${failure.javaClass.simpleName})")
                    completed.countDown()
                }
            }
            completed.await(timeoutSeconds, TimeUnit.SECONDS)
            return result.get()
        } finally {
            instrumentation.runOnMainSync { recognizer?.destroy() }
        }
    }

    private fun probeTts(): String {
        var engine: TextToSpeech? = null
        val status = AtomicReference<Int?>(null)
        val initialized = CountDownLatch(1)
        try {
            instrumentation.runOnMainSync {
                engine = TextToSpeech(context) { code ->
                    status.set(code)
                    initialized.countDown()
                }
            }
            if (!initialized.await(timeoutSeconds, TimeUnit.SECONDS)) return "UNKNOWN(engine init timeout)"
            if (status.get() != TextToSpeech.SUCCESS) return "ERROR(engine init=${status.get()})"
            val voices = engine!!.voices ?: return "UNKNOWN(engine returned no voice inventory)"
            val vietnamese = voices.filter { it.locale == Locale.forLanguageTag("vi-VN") }
            val offline = vietnamese.filter { !it.isNetworkConnectionRequired }
            // FEATURE_NOT_INSTALLED means advertised data is missing; never install it here.
            val installed = offline.filter {
                TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED !in it.features.orEmpty()
            }
            val state = if (installed.isNotEmpty())
                "AVAILABLE(engine advertises installed offline voices; synthesis untested)"
            else "UNAVAILABLE(no installed offline exact-locale voice advertised)"
            return "$state engine=${engine!!.defaultEngine} offline=${installed.map { it.name }} " +
                "missingData=${offline.size - installed.size} network=${vietnamese.count { it.isNetworkConnectionRequired }}"
        } catch (failure: RuntimeException) {
            return "ERROR(${failure.javaClass.simpleName})"
        } finally {
            instrumentation.runOnMainSync { engine?.shutdown() }
        }
    }
}
