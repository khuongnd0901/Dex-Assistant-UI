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
    private val stream = spotter.createStream()

    fun accept(samples: ShortArray, count: Int): Boolean {
        val normalized = FloatArray(count) { samples[it] / 32768f }
        stream.acceptWaveform(normalized, 16_000)
        while (spotter.isReady(stream)) {
            spotter.decode(stream)
            if (spotter.getResult(stream).keyword.isNotBlank()) return true
        }
        return false
    }
    override fun close() { stream.release(); spotter.release() }
}
