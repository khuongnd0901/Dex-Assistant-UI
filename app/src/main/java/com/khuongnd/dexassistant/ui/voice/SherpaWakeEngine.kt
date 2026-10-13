package com.khuongnd.dexassistant.ui.voice

import android.content.res.AssetManager
import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.KeywordSpotter
import com.k2fsa.sherpa.onnx.KeywordSpotterConfig
import com.k2fsa.sherpa.onnx.OnlineModelConfig
import com.k2fsa.sherpa.onnx.OnlineTransducerModelConfig

/** One worker owns this native object and its stream, including their releases. */
class SherpaWakeEngine(assets: AssetManager) : AutoCloseable {
    private val spotter = KeywordSpotter(assets, KeywordSpotterConfig(
        featConfig = FeatureConfig(sampleRate = 16_000, featureDim = 80),
        modelConfig = OnlineModelConfig(
            transducer = OnlineTransducerModelConfig(
                encoder = "kws/encoder-epoch-12-avg-2-chunk-16-left-64.int8.onnx",
                decoder = "kws/decoder-epoch-12-avg-2-chunk-16-left-64.int8.onnx",
                joiner = "kws/joiner-epoch-12-avg-2-chunk-16-left-64.int8.onnx",
            ),
            tokens = "kws/tokens.txt", numThreads = 1, provider = "cpu", modelType = "zipformer2",
        ), keywordsFile = "kws/keywords.txt",
    ))
    private var stream = spotter.createStream()
    private val pcm = WakePcmBuffer()
    private var closed = false
    /** Worker-confined; no audio or decoder history survives between listening epochs. */
    fun resetSession() {
        check(!closed)
        pcm.clear()
        val fresh = spotter.createStream()
        stream.release()
        stream = fresh
    }

    fun accept(samples: ShortArray, count: Int): Boolean {
        check(!closed)
        val normalized = pcm.convert(samples, count)
        stream.acceptWaveform(normalized, 16_000)
        while (spotter.isReady(stream)) {
            spotter.decode(stream)
            if (spotter.getResult(stream).keyword.isNotBlank()) return true
        }
        return false
    }
    /** Clear session audio on capture-loop exit while keeping the warm native model. */
    internal fun clearCapturedFrame() { pcm.clear() }
    override fun close() {
        if (closed) return
        closed = true; pcm.clear(); stream.release(); spotter.release()
    }
}
