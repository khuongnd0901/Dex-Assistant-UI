package com.khuongnd.dexassistant.dpc

object PermissionPolicy {
    const val GRANTED = "GRANTED"
    const val USER_ACTION_REQUIRED = "USER_ACTION_REQUIRED"
    const val UNSUPPORTED = "UNSUPPORTED"
    const val FAILED = "FAILED"

    fun classify(requested: Boolean, dangerous: Boolean, currentlyGranted: Boolean, grantSucceeded: Boolean?): String = when {
        !requested || !dangerous -> UNSUPPORTED
        currentlyGranted -> GRANTED
        grantSucceeded == true -> GRANTED
        grantSucceeded == false -> FAILED
        else -> USER_ACTION_REQUIRED
    }
}
