package com.khuongnd.dexassistant.ui.voice

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast

object VoiceButtonActions {
    fun supported(action: String?): Boolean = action == Intent.ACTION_VOICE_COMMAND ||
        action == Intent.ACTION_ASSIST || action == RecognizerIntent.ACTION_WEB_SEARCH
}

/** Entry for hands-free devices that launch speech WEB_SEARCH/VOICE_COMMAND instead of a session. */
class VoiceButtonActivity : Activity() {
    private var handled = false
    override fun onResume() {
        super.onResume()
        if (!handled) {
            handled = true
            // Ignore all external extras. Permissions, unlock, busy state and suppression are rechecked.
            if (VoiceButtonActions.supported(intent?.action) && !VoiceCommandActivation.request(this)) {
                val state = VoiceAssistantService.active?.currentVoiceState()
                val message = if (state != VoiceState.PAUSED && !VoiceActivationGate.idle(state))
                    "Trợ lý đang xử lý lệnh. Hãy đợi một chút."
                else "Trợ lý chưa sẵn sàng. Mở khóa và bật nghe trong DeX Assistant."
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
        }
        finish()
    }
}
