package com.khuongnd.dexassistant.ui.voice

/** A bounded startup gate: a transport success alone does not establish ownership. */
class PermissionReconciliation(private val maxAttempts: Int = 3) {
    var complete = false
        private set
    var pending = false
        private set
    private var attempts = 0

    fun begin(): Boolean {
        if (pending || complete || attempts >= maxAttempts) return false
        attempts++
        pending = true
        return true
    }

    fun finish(status: String, ownerGranted: Boolean): Boolean {
        pending = false
        complete = status == "SUCCESS" && ownerGranted
        return complete
    }

    fun canRetry() = !complete && attempts < maxAttempts
}
