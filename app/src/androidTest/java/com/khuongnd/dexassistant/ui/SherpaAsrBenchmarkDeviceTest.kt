package com.khuongnd.dexassistant.ui

import android.app.UiAutomation
import android.os.Build
import android.os.Debug
import android.os.Process
import android.os.SystemClock
import androidx.test.platform.app.InstrumentationRegistry
import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import com.k2fsa.sherpa.onnx.OfflineTransducerModelConfig
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest

/**
 * T-017: explicit opt-in, non-executing, on-device WAV inference.
 * No microphone, network, action dispatch, permission changes or production voice orchestration.
 * Files live in the TARGET application's external-files directory, outside git/APK.
 * Never run this test in an ordinary broad device suite: it opt-in-skips by default.
 */
class SherpaAsrBenchmarkDeviceTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext

    @Test fun benchmarkOfflineVietnameseAsrOnFold3() {
        val args = InstrumentationRegistry.getArguments()
        assumeTrue("Explicit benchmark opt-in is required",
            args.getString("runSherpaAsrBenchmark") == "true")
        // Safeguard the user's enabled Accessibility services during instrumentation.
        instrumentation.getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
        assertTrue("Physical Galaxy Z Fold3 required; AVD is not performance evidence",
            Build.MODEL.startsWith("SM-F926") && Build.SUPPORTED_ABIS.contains("arm64-v8a"))

        val storage = args.getString("sherpaBenchmarkStorage", "external")
        require(storage == "external" || storage == "internal") {
            "sherpaBenchmarkStorage must be external or internal"
        }
        // Internal fallback supports devices where instrumentation's scoped-storage
        // mount cannot read target external files populated by shell. Main stages
        // files with target run-as; this test never copies or changes permissions.
        val base = if (storage == "internal") File(context.filesDir, "sherpa-vi-benchmark")
            else requireNotNull(context.getExternalFilesDir(null))
        instrumentation.sendStatus(0, android.os.Bundle().apply {
            putString("sherpaBenchmarkTargetPackage", context.packageName)
            putString("sherpaBenchmarkInstrumentationPackage", instrumentation.context.packageName)
            putString("sherpaBenchmarkBase", base.absolutePath)
            putString("sherpaBenchmarkStorage", storage)
            putInt("sherpaBenchmarkUid", Process.myUid())
        })
        val model = File(base, "sherpa-vi-model")
        val realSession = args.getString("sherpaRealCorpusSession")
        require(realSession == null || realSession.matches(Regex("session-[0-9]{13}"))) {
            "sherpaRealCorpusSession must be a collector session ID"
        }
        val corpus = if (realSession == null) File(base, "sherpa-vi-wav")
            else File(context.filesDir, "sherpa-vi-real-corpus/$realSession")
        val output = File(base, "sherpa-vi-results").apply { mkdirs() }
        val required = listOf("encoder.int8.onnx", "decoder.onnx", "joiner.int8.onnx", "tokens.txt")
        required.forEach {
            val file = File(model, it)
            require(file.isFile && file.canRead()) {
                "Unreadable benchmark model: path=${file.absolutePath}; " +
                    "target=${context.packageName}; instrumentation=${instrumentation.context.packageName}; " +
                    "uid=${Process.myUid()}; storage=$storage; " +
                    "modelDirExists=${model.exists()}; modelDirReadable=${model.canRead()}; " +
                    "fileExists=${file.exists()}; isFile=${file.isFile}; readable=${file.canRead()}"
            }
        }
        val wavs = corpus.listFiles { file ->
            file.isFile && file.name.endsWith(".wav", ignoreCase = true)
        }?.sortedBy { it.name } ?: emptyList()
        require(wavs.isNotEmpty()) { "No WAV files in ${corpus.absolutePath}" }
        require(wavs.size <= 200) { "Limit benchmark to 200 WAVs per run" }
        val manifest = JSONObject().apply {
            put("task", "T-017")
            put("model", "sherpa-onnx-zipformer-vi-30M-int8-2026-02-09")
            put("runtime", "sherpa-onnx-1.13.8")
            put("device", Build.MODEL)
            put("sdk", Build.VERSION.SDK_INT)
            put("abi", Build.SUPPORTED_ABIS.firstOrNull().orEmpty())
            put("clock", "SystemClock.elapsedRealtimeNanos")
            put("note", "Explicit offline WAV only; NOT a Google STT comparison or live voice result")
            put("warmupsPerThread", 2)
            put("measuredRepeats", 5)
            put("modelLoadKind", "first-observed per thread profile; filesystem/cache not cold-controlled")
            put("coldLoadMeasured", false)
            put("wavCount", wavs.size)
            put("corpusKind", if (realSession == null) "staged_wav" else "consented_real_pilot")
            put("realCorpusSession", realSession ?: JSONObject.NULL)
        }
        val sha = JSONObject()
        val trusted = JSONObject(File(model, "verified-model.json").readText())
        require(trusted.getString("revision") == "83e140db6d23fbb8480fd5fb868f74ab80e7092c") {
            "Model provenance revision mismatch"
        }
        val expectedHashes = trusted.getJSONObject("sha256")
        required.forEach {
            val actual = sha256(File(model, it))
            require(actual == expectedHashes.getString(it)) { "Model integrity mismatch: $it" }
            sha.put(it, actual)
        }
        manifest.put("modelRevision", trusted.getString("revision"))
        manifest.put("modelSha256", sha)
        val rows = JSONArray()
        for (threads in listOf(1, 2, 4)) {
            val config = OfflineRecognizerConfig(
                featConfig = FeatureConfig(sampleRate = 16000, featureDim = 80),
                modelConfig = OfflineModelConfig(
                    transducer = OfflineTransducerModelConfig(
                        encoder = File(model, "encoder.int8.onnx").absolutePath,
                        decoder = File(model, "decoder.onnx").absolutePath,
                        joiner = File(model, "joiner.int8.onnx").absolutePath,
                    ),
                    tokens = File(model, "tokens.txt").absolutePath,
                    numThreads = threads,
                    provider = "cpu",
                )
            )
            val loadStart = SystemClock.elapsedRealtimeNanos()
            val recognizer = OfflineRecognizer(config = config)
            val loadMs = nanosMs(SystemClock.elapsedRealtimeNanos() - loadStart)
            try {
                val warmupAudio = readWav16k(wavs.first())
                repeat(2) { decodeOnce(recognizer, warmupAudio) }
                for (wav in wavs) {
                    val samples = readWav16k(wav)
                    val audioSeconds = samples.size / 16000.0
                    repeat(5) { repetition ->
                        val pssBeforeKb = currentPssKb()
                        val cpuStartMs = Process.getElapsedCpuTime()
                        val start = SystemClock.elapsedRealtimeNanos()
                        val transcript = decodeOnce(recognizer, samples)
                        val decodeMs = nanosMs(SystemClock.elapsedRealtimeNanos() - start)
                        val cpuMs = Process.getElapsedCpuTime() - cpuStartMs
                        val pssAfterKb = currentPssKb()
                        rows.put(JSONObject().apply {
                            put("sampleId", wav.nameWithoutExtension)
                            put("threads", threads)
                            put("repeat", repetition + 1)
                            put("modelLoadMs", loadMs)
                            put("audioSeconds", audioSeconds)
                            put("decodeMs", decodeMs)
                            put("rtf", decodeMs / (audioSeconds * 1000))
                            put("processCpuMs", cpuMs)
                            put("processPssBeforeKb", pssBeforeKb)
                            put("processPssAfterKb", pssAfterKb)
                            put("hypothesis", transcript)
                        })
                    }
                }
            } finally {
                recognizer.release()
            }
        }
        val report = JSONObject().put("metadata", manifest).put("measurements", rows)
        val file = File(output, "wav-benchmark-${System.currentTimeMillis()}.json")
        file.writeText(report.toString(2))
        // Only the local path, not recognized audio/transcripts, is emitted to logs.
        instrumentation.sendStatus(0, android.os.Bundle().apply {
            putString("sherpaBenchmarkReport", file.absolutePath)
            putInt("measurementCount", rows.length())
        })
    }

    private fun decodeOnce(recognizer: OfflineRecognizer, samples: FloatArray): String {
        val stream = recognizer.createStream()
        return try {
            stream.acceptWaveform(samples, 16000)
            recognizer.decode(stream)
            recognizer.getResult(stream).text
        } finally {
            stream.release()
        }
    }

    private fun readWav16k(file: File): FloatArray {
        require(file.length() in 44..2_000_000L) { "WAV must be <= 60s mono 16k/16-bit: ${file.name}" }
        val bytes = file.readBytes()
        val b = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        fun fourcc(offset: Int) = String(bytes, offset, 4, Charsets.US_ASCII)
        require(fourcc(0) == "RIFF" && fourcc(8) == "WAVE") { "Not RIFF/WAVE: ${file.name}" }
        var offset = 12
        var validFormat = false
        var dataStart = -1
        var dataLength = -1
        while (offset + 8 <= bytes.size) {
            val length = b.getInt(offset + 4)
            require(length >= 0 && offset + 8L + length <= bytes.size) { "Invalid WAV chunk" }
            when (fourcc(offset)) {
                "fmt " -> {
                    require(length >= 16) { "Invalid fmt chunk" }
                    validFormat = b.getShort(offset + 8).toInt() == 1 &&
                        b.getShort(offset + 10).toInt() == 1 &&
                        b.getInt(offset + 12) == 16000 &&
                        b.getShort(offset + 22).toInt() == 16
                }
                "data" -> { dataStart = offset + 8; dataLength = length }
            }
            offset += 8 + length + (length and 1)
        }
        require(validFormat && dataStart >= 0 && dataLength in 320..1_920_000 &&
            dataLength % 2 == 0) { "Expected PCM16 mono 16k WAV: ${file.name}" }
        return FloatArray(dataLength / 2) { i ->
            b.getShort(dataStart + i * 2) / 32768f
        }
    }

    private fun currentPssKb(): Int = Debug.MemoryInfo().also(Debug::getMemoryInfo).totalPss
    private fun nanosMs(nanos: Long): Double = nanos / 1_000_000.0

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { stream ->
            val buffer = ByteArray(32768)
            while (true) {
                val n = stream.read(buffer)
                if (n < 0) break
                digest.update(buffer, 0, n)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
