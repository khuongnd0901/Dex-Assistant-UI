package com.khuongnd.dexassistant.ui.voice

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.service.voice.VoiceInteractionSession
import android.service.voice.VoiceInteractionSessionService

class AssistantVoiceInteractionSessionService : VoiceInteractionSessionService() {
    override fun onNewSession(args: Bundle?): VoiceInteractionSession = object : VoiceInteractionSession(this) {
        override fun onPrepareShow(args: Bundle?, showFlags: Int) {
            super.onPrepareShow(args, showFlags)
            setUiEnabled(false) // Keep the foreground app available to the existing screen pipeline.
        }
        override fun onShow(args: Bundle?, showFlags: Int) {
            super.onShow(args, showFlags)
            hide()
            Handler(Looper.getMainLooper()).post {
                VoiceCommandActivation.request(this@AssistantVoiceInteractionSessionService)
            }
        }
    }
}
