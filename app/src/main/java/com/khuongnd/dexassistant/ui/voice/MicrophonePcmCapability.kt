package com.khuongnd.dexassistant.ui.voice

/** Measured source-segmented PCM support; an API extra alone is not a capability claim. */
internal object MicrophonePcmCapability {
    fun supports(sdk: Int, packageName: String, serviceName: String, version: Long): Boolean =
        sdk >= 33 && packageName == "com.google.android.tts" &&
            serviceName == "com.google.android.apps.speech.tts.googletts.service.GoogleTTSRecognitionService" &&
            version == 210673049L
}
