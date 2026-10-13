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
    private data class Candidate(val component: ComponentName, val system: Boolean)
    private var catalog: List<Candidate>? = null
    private var receiverRegistered = false
    /** Package enable/export/install changes invalidate discovery; Secure selection is read every time. */
    @Synchronized internal fun invalidateCatalog() { catalog = null }

    /** Excludes our assistant-contract stub; retains only a bounded package catalog, never a recognizer. */
    @Synchronized fun component(context: Context): ComponentName? {
        if (!receiverRegistered) {
            val filter = android.content.IntentFilter().apply {
                addAction(Intent.ACTION_PACKAGE_ADDED); addAction(Intent.ACTION_PACKAGE_REMOVED)
                addAction(Intent.ACTION_PACKAGE_CHANGED); addAction(Intent.ACTION_PACKAGE_REPLACED)
                addDataScheme("package")
            }
            androidx.core.content.ContextCompat.registerReceiver(context.applicationContext,
                object : android.content.BroadcastReceiver() {
                    override fun onReceive(context: Context, intent: Intent) = invalidateCatalog()
                }, filter, androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED)
            receiverRegistered = true
        }
        val services = catalog ?: context.packageManager.queryIntentServices(
            Intent(RecognitionService.SERVICE_INTERFACE), 0,
        ).mapNotNull { it.serviceInfo }.filter { it.packageName != context.packageName && it.exported }
            .map { Candidate(ComponentName(it.packageName, it.name),
                it.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0) }
            .sortedWith(compareByDescending<Candidate> { it.system }
                .thenBy { it.component.packageName }.thenBy { it.component.className })
            .also { catalog = it }
        val configured = Settings.Secure.getString(context.contentResolver, "voice_recognition_service")
            ?.let(ComponentName::unflattenFromString)
        return services.firstOrNull { configured == it.component }?.component ?: services.firstOrNull()?.component
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
