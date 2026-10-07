package com.khuongnd.dexassistant.ui.voice

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.provider.Settings
import android.speech.RecognitionService
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

object CommandRecognizer {
    /** Excludes our required assistant-contract stub; it is not a command STT engine. */
    fun component(context: Context): ComponentName? {
        val services = context.packageManager.queryIntentServices(
            Intent(RecognitionService.SERVICE_INTERFACE), 0,
        ).mapNotNull { it.serviceInfo }.filter { it.packageName != context.packageName && it.exported }
        val configured = Settings.Secure.getString(context.contentResolver, "voice_recognition_service")
            ?.let(ComponentName::unflattenFromString)
        services.firstOrNull { configured == ComponentName(it.packageName, it.name) }?.let {
            return ComponentName(it.packageName, it.name)
        }
        return services.sortedWith(compareByDescending<android.content.pm.ServiceInfo> {
            it.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0
        }.thenBy { it.packageName }.thenBy { it.name }).firstOrNull()?.let {
            ComponentName(it.packageName, it.name)
        }
    }

    fun intent(commandEndpointHints: Boolean = true): Intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "vi-VN")
        .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        // Explicitly permit the selected service's online path. This is a service hint.
        .putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false)
        .also { intent ->
            // Controlled command endpointer trial. Services may ignore these hints;
            // only a final callback or explicit mic submit commits text.
            if (commandEndpointHints) {
                intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1_500L)
                intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1_000L)
            }
        }

    fun errorCode(error: Int): String = when (error) {
        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "STT_NETWORK"
        SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "STT_NO_SPEECH"
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "STT_PERMISSION_DENIED"
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "STT_BUSY"
        SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED -> "STT_VI_UNSUPPORTED"
        SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> "STT_VI_UNAVAILABLE"
        else -> "STT_ERROR_$error"
    }
}
