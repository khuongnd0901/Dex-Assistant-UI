package com.khuongnd.dexassistant.ui

import android.annotation.SuppressLint
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.test.platform.app.InstrumentationRegistry
import com.khuongnd.dexassistant.ui.voice.CommandRecognizer
import com.khuongnd.dexassistant.ui.voice.CommandMicrophoneSource
import android.os.Handler
import android.os.Looper
import com.khuongnd.dexassistant.ui.voice.VoiceAssistantService
import com.khuongnd.dexassistant.ui.voice.VoiceState
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.Before
import org.junit.After
import android.content.Intent
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/** Selected diagnostics only. No commands, history, global routing or audio persistence. */
class MicrophoneRoutingDeviceTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private var resumeListening = false
    private var originalPolicy: Map<String, *> = emptyMap<String, Any>()
    @Before fun pauseNormalListening() {
        instrumentation.getUiAutomation(android.app.UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
        originalPolicy = context.getSharedPreferences("voice_policy", 0).all
        instrumentation.runOnMainSync {
            resumeListening = VoiceAssistantService.active?.currentVoiceState() == VoiceState.LISTENING
            if (resumeListening) context.startService(Intent(context, VoiceAssistantService::class.java)
                .setAction(VoiceAssistantService.ACTION_PAUSE))
        }
        if (resumeListening) {
            val until = android.os.SystemClock.elapsedRealtime() + 5000
            while (android.os.SystemClock.elapsedRealtime() < until) {
                var paused = false
                instrumentation.runOnMainSync { paused = VoiceAssistantService.active?.currentVoiceState() == VoiceState.PAUSED }
                if (paused) break
                Thread.sleep(50)
            }
            Thread.sleep(300) // stop wakes blocked read; worker releases before isolated capture.
        }
    }
    @After fun restoreNormalListening() {
        if (resumeListening) {
            instrumentation.runOnMainSync { context.startService(Intent(context, VoiceAssistantService::class.java)
                .setAction(VoiceAssistantService.ACTION_RESUME)) }
            val until = android.os.SystemClock.elapsedRealtime() + 15000
            while (android.os.SystemClock.elapsedRealtime() < until &&
                !context.getSharedPreferences(VoiceAssistantService.HEALTH_PREFERENCES, 0)
                    .getString(VoiceAssistantService.KEY_DETAIL, "").orEmpty().startsWith("Listening for Hey Hey offline")) Thread.sleep(50)
        }
        assertEquals("Normal pause/resume must preserve owner policy", originalPolicy,
            context.getSharedPreferences("voice_policy", 0).all)
    }
    private fun requireIdle() {
        instrumentation.getUiAutomation(android.app.UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
        var idle = false
        instrumentation.runOnMainSync { idle = VoiceAssistantService.active?.currentVoiceState() in
            listOf(null, VoiceState.STOPPED, VoiceState.PAUSED, VoiceState.ERROR) }
        assumeTrue("Pause voice before isolated routing diagnostics", idle)
    }

    @SuppressLint("MissingPermission")
    @Test fun enumeratedInputsReportActualRouteAndReleaseSequentially() {
        requireIdle()
        val manager = context.getSystemService(AudioManager::class.java)
        val devices = manager.getDevices(AudioManager.GET_DEVICES_INPUTS).filter {
            it.type in setOf(AudioDeviceInfo.TYPE_BUILTIN_MIC, AudioDeviceInfo.TYPE_USB_DEVICE,
                AudioDeviceInfo.TYPE_USB_HEADSET, AudioDeviceInfo.TYPE_WIRED_HEADSET)
        }.take(8)
        assertTrue(devices.isNotEmpty())
        for (device in devices) {
            val input = AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, 16000,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
                AudioRecord.getMinBufferSize(16000, AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT).coerceAtLeast(3200))
            try {
                assertEquals(AudioRecord.STATE_INITIALIZED, input.state)
                val accepted = input.setPreferredDevice(device)
                input.startRecording()
                val frame = ShortArray(320)
                repeat(25) { assertTrue(input.read(frame, 0, frame.size) > 0) }
                val actual = input.routedDevice
                android.util.Log.i("DexMicrophoneTest", "requested=${device.id} type=${device.type} accepted=$accepted actual=${actual?.id ?: 0} silenced=${input.activeRecordingConfiguration?.isClientSilenced}")
                assertNotNull(actual)
            } finally { runCatching { input.stop() }; input.release() }
        }
    }

    @Test fun cancelledUnreadSourceReleasesWorkerAndRecorder() {
        requireIdle()
        val manager = context.getSystemService(AudioManager::class.java)
        val baseline = manager.activeRecordingConfigurations.map { it.clientAudioSessionId }.toSet()
        val prepared = CountDownLatch(1)
        val failures = AtomicInteger()
        val worker = java.util.concurrent.Executors.newSingleThreadExecutor()
        val source = CommandMicrophoneSource(context, null, Handler(Looper.getMainLooper()), worker,
            ready = { _, type ->
                if (type == AudioDeviceInfo.TYPE_BUILTIN_MIC) prepared.countDown()
            }, failed = { failures.incrementAndGet(); prepared.countDown() })
        try {
            source.start()
            assertTrue("Source did not prepare phone input", prepared.await(5, TimeUnit.SECONDS))
            assertEquals(0, failures.get())
            Thread.sleep(1000) // Deliberately leave its reader open/unread: exercise backpressure.
            source.close()
            worker.submit {}.get(2, TimeUnit.SECONDS) // Must run after capture's finally releases.
            assertEquals(baseline, manager.activeRecordingConfigurations.map { it.clientAudioSessionId }.toSet())
            assertEquals("Cancellation must suppress late failure", 0, failures.get())
        } finally {
            source.close()
            worker.shutdownNow()
            worker.awaitTermination(2, TimeUnit.SECONDS)
        }
    }

    @SuppressLint("MissingPermission")
    @Test fun simultaneousBuiltInAndUsbReportIndependentCaptureCapability() {
        requireIdle()
        val inputs = context.getSystemService(AudioManager::class.java).getDevices(AudioManager.GET_DEVICES_INPUTS)
        val builtin = inputs.filter { it.type == AudioDeviceInfo.TYPE_BUILTIN_MIC }.minByOrNull { it.id }
        val usb = inputs.firstOrNull { it.type in setOf(AudioDeviceInfo.TYPE_USB_DEVICE, AudioDeviceInfo.TYPE_USB_HEADSET) }
        assumeTrue("Connect USB microphone", builtin != null && usb != null)
        val devices = listOf(builtin!!, usb!!)
        val records = mutableListOf<AudioRecord>()
        val workers = java.util.concurrent.Executors.newFixedThreadPool(2)
        try {
            for (device in devices) {
                val record = AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, 16000,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
                    AudioRecord.getMinBufferSize(16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT).coerceAtLeast(3200))
                records.add(record)
                assertEquals(AudioRecord.STATE_INITIALIZED, record.state)
                assertTrue(record.setPreferredDevice(device))
                record.startRecording()
            }
            val futures = records.map { record -> workers.submit {
                val frame = ShortArray(320)
                repeat(50) { assertTrue(record.read(frame, 0, frame.size) > 0) }
            } }
            futures.forEach { it.get(5, TimeUnit.SECONDS) }
            val independent = records.indices.all { index ->
                records[index].routedDevice?.id == devices[index].id &&
                    records[index].activeRecordingConfiguration?.isClientSilenced == false
            }
            records.indices.forEach { index ->
                android.util.Log.i("DexMicrophoneTest", "concurrent_stream=$index requested=${devices[index].id} actual=${records[index].routedDevice?.id ?: 0} silenced=${records[index].activeRecordingConfiguration?.isClientSilenced}")
            }
            android.util.Log.i("DexMicrophoneTest", "concurrent_independent=$independent")
        } finally {
            records.forEach { runCatching { it.stop() } }
            workers.shutdownNow()
            workers.awaitTermination(2, TimeUnit.SECONDS)
            records.forEach { it.release() }
        }
    }

    @Test fun selectedProviderRecognizesKnownSuppliedPcm() {
        requireIdle()
        assumeTrue(android.os.Build.VERSION.SDK_INT >= 33)
        val component = CommandRecognizer.component(context)
        assumeTrue(component != null)
        val initialized = CountDownLatch(1)
        val generated = CountDownLatch(1)
        val status = AtomicInteger(-1)
        val pcm = ByteArrayOutputStream()
        val rate = AtomicInteger(16000)
        val channels = AtomicInteger(1)
        val format = AtomicInteger(AudioFormat.ENCODING_PCM_16BIT)
        var tts: TextToSpeech? = null
        val temporary = File(context.cacheDir, "t019-provider-proof-${System.nanoTime()}.wav")
        try {
            instrumentation.runOnMainSync {
                tts = TextToSpeech(context) { status.set(it); initialized.countDown() }
            }
            assertTrue(initialized.await(15, TimeUnit.SECONDS))
            assertEquals(TextToSpeech.SUCCESS, status.get())
            instrumentation.runOnMainSync {
                tts!!.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(id: String?) {}
                    override fun onDone(id: String?) { generated.countDown() }
                    @Deprecated("platform callback") override fun onError(id: String?) { generated.countDown() }
                    override fun onBeginSynthesis(id: String?, sampleRate: Int, audioFormat: Int, channelCount: Int) {
                        rate.set(sampleRate); format.set(audioFormat); channels.set(channelCount)
                    }
                    override fun onAudioAvailable(id: String?, audio: ByteArray?) {
                        if (audio != null && pcm.size() + audio.size <= 2_000_000) pcm.write(audio)
                    }
                })
                tts!!.language = Locale.forLanguageTag("vi-VN")
                val offline = tts!!.voices.firstOrNull { it.locale.language == "vi" && !it.isNetworkConnectionRequired }
                assertNotNull("Offline Vietnamese TTS voice required", offline)
                assertEquals(TextToSpeech.SUCCESS, tts!!.setVoice(offline))
                assertEquals(TextToSpeech.SUCCESS, tts!!.synthesizeToFile("kiểm tra micro tự động. kiểm tra micro tự động.", Bundle(), temporary, "t019"))
            }
            assertTrue(generated.await(15, TimeUnit.SECONDS))
            assertTrue("Offline Vietnamese TTS must produce PCM", pcm.size() > 0)
            assertEquals(AudioFormat.ENCODING_PCM_16BIT, format.get())
            val originalAudio = pcm.toByteArray()
            val originalRate = rate.get()
            if (originalRate != 16000 && channels.get() == 1) {
                pcm.reset()
                val sourceSamples = originalAudio.size / 2
                val outputSamples = sourceSamples.toLong() * 16000 / originalRate
                for (i in 0 until outputSamples.toInt()) {
                    val source = (i.toLong() * originalRate / 16000).toInt().coerceAtMost(sourceSamples - 1)
                    pcm.write(originalAudio[source * 2].toInt())
                    pcm.write(originalAudio[source * 2 + 1].toInt())
                }
                rate.set(16000)
            }
            val generatedAudio = pcm.toByteArray()
            var energy = 0.0
            for (i in 0 until generatedAudio.size - 1 step 2) {
                val sample = ((generatedAudio[i].toInt() and 255) or (generatedAudio[i + 1].toInt() shl 8)).toShort().toDouble()
                energy += sample * sample
            }
            val generatedRms = kotlin.math.sqrt(energy / (generatedAudio.size / 2)) / 32768
            assertTrue("Synthetic proof audio must be audible", generatedRms > 0.005)
            android.util.Log.i("DexMicrophoneTest", "synthetic_bytes=${generatedAudio.size} sample_rate=${rate.get()} channels=${channels.get()} audible=true segmented=true")
            val pipe = ParcelFileDescriptor.createPipe()
            val ended = CountDownLatch(1)
            val result = AtomicReference<String>()
            var recognizer: SpeechRecognizer? = null
            val writer = Thread {
                runCatching { ParcelFileDescriptor.AutoCloseOutputStream(pipe[1]).use { output ->
                    val audio = pcm.toByteArray()
                    val stride = rate.get() * channels.get() * 2 / 50
                    var offset = 0
                    while (offset < audio.size) {
                        val count = minOf(stride, audio.size - offset)
                        output.write(audio, offset, count); offset += count
                        Thread.sleep(20)
                    }
                    output.write(ByteArray(rate.get() * channels.get() * 2 * 2))
                } }
            }
            try {
                instrumentation.runOnMainSync {
                    recognizer = SpeechRecognizer.createSpeechRecognizer(context, component)
                    recognizer!!.setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {}
                        override fun onBeginningOfSpeech() {}
                        override fun onRmsChanged(rmsdB: Float) {}
                        override fun onBufferReceived(buffer: ByteArray?) {}
                        override fun onEndOfSpeech() {}
                        override fun onError(error: Int) { result.set("ERROR_$error"); ended.countDown() }
                        override fun onResults(results: Bundle?) {
                            val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                            if (text.isNotBlank()) result.set(text)
                            ended.countDown()
                        }
                        override fun onSegmentResults(segmentResults: Bundle) {
                            val text = segmentResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                            if (text.isNotBlank()) result.set(text)
                        }
                        override fun onEndOfSegmentedSession() { ended.countDown() }
                        override fun onPartialResults(partialResults: Bundle?) {}
                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })
                    recognizer!!.startListening(CommandRecognizer.intent(false)
                        .putExtra(RecognizerIntent.EXTRA_SEGMENTED_SESSION, RecognizerIntent.EXTRA_AUDIO_SOURCE)
                        .putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE, pipe[0])
                        .putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_SAMPLING_RATE, rate.get())
                        .putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_CHANNEL_COUNT, channels.get())
                        .putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_ENCODING, format.get()))
                }
                writer.start()
                assertTrue("Provider PCM callback timed out", ended.await(30, TimeUnit.SECONDS))
                val normalized = java.text.Normalizer.normalize(result.get().orEmpty().lowercase(Locale.ROOT),
                    java.text.Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "").replace(Regex("[^a-z0-9]+"), " ").trim()
                val accepted = normalized.contains("kiem tra")
                android.util.Log.i("DexMicrophoneTest", "provider=${component!!.packageName} external_pcm=$accepted result_chars=${result.get().orEmpty().length} known_kiem=${normalized.contains("kiem")} known_tra=${normalized.contains("tra")} status=${if (result.get().orEmpty().startsWith("ERROR_")) result.get() else "RESULT"}")
                assertTrue("Selected provider did not recognize supplied known PCM", accepted)
            } finally {
                instrumentation.runOnMainSync { recognizer?.cancel(); recognizer?.destroy() }
                pipe.forEach { runCatching { it.close() } }
                writer.interrupt(); writer.join(2000)
            }
        } finally {
            instrumentation.runOnMainSync { tts?.stop(); tts?.shutdown() }
            temporary.delete()
        }
    }
}
