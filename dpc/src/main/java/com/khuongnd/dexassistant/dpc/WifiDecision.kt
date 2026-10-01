package com.khuongnd.dexassistant.dpc

object WifiDecision {
    fun preflight(isDeviceOwner: Boolean, requestAccepted: Boolean): Result = when {
        !isDeviceOwner -> Result("REJECTED", "DEVICE_OWNER_REQUIRED")
        !requestAccepted -> Result("FAILED", "POLICY_DENIED")
        else -> Result("PENDING", null)
    }

    data class Result(val status: String, val errorCode: String?)
}
