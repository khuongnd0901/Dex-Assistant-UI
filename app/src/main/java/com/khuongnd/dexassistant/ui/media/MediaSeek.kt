package com.khuongnd.dexassistant.ui.media

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.SystemClock
import android.provider.Settings
import com.khuongnd.dexassistant.ui.actions.LocalOutcome
import com.khuongnd.dexassistant.ui.actions.LocalResult

class MediaSeek(private val context: Context) {
    private val component = ComponentName(context, CommandNotificationListener::class.java)
    fun hasAccess(): Boolean = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        .orEmpty().split(':').any { ComponentName.unflattenFromString(it) == component }
    fun execute(operation: String, seconds: Int): LocalResult {
        if (operation !in setOf("FORWARD", "BACKWARD") || seconds !in 1..3600) return LocalResult(LocalOutcome.REJECTED, "INVALID_ARGUMENTS")
        if (!hasAccess()) return LocalResult(LocalOutcome.REJECTED, "NOTIFICATION_ACCESS_REQUIRED")
        return runCatching {
            val sessions = context.getSystemService(MediaSessionManager::class.java).getActiveSessions(component)
            val candidates = sessions.filter { session ->
                session.packageName in setOf("com.google.android.youtube", "com.google.android.apps.youtube.music") && session.playbackState?.let { state ->
                state.actions and PlaybackState.ACTION_SEEK_TO != 0L && state.position >= 0 &&
                    state.state in setOf(PlaybackState.STATE_PLAYING, PlaybackState.STATE_PAUSED)
            } == true }
            if (candidates.size != 1) return LocalResult(LocalOutcome.REJECTED,
                if (candidates.isEmpty()) "CAPABILITY_UNAVAILABLE" else "AMBIGUOUS_MEDIA_SESSION")
            val session = candidates.single()
            val state = session.playbackState ?: return LocalResult(LocalOutcome.FAILED, "DEVICE_STATE_CHANGED")
            val duration = session.metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION)
            session.transportControls.seekTo(SeekBounds.target(state.position,
                if (state.lastPositionUpdateTime > 0) SystemClock.elapsedRealtime() - state.lastPositionUpdateTime else 0,
                state.playbackSpeed, state.state == PlaybackState.STATE_PLAYING, duration, seconds, operation == "FORWARD"))
            LocalResult(LocalOutcome.DISPATCHED)
        }.getOrElse { LocalResult(LocalOutcome.FAILED, "CAPABILITY_UNAVAILABLE") }
    }
}
