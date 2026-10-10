package com.khuongnd.dexassistant.ui.voice

import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.AudioRecord

/** Worker-confined. Only one AudioRecord exists; no global audio/communication settings changed. */
internal class WakeMicrophoneRouter(private val manager: AudioManager, private val input: AudioRecord) {
    private val policy = MicrophoneScanPolicy()
    private val rejected = mutableSetOf<Int>()
    private var candidates = emptyList<AudioDeviceInfo>()
    private var refreshedAt = Long.MIN_VALUE
    private var requested: AudioDeviceInfo? = null
    private var changedAt = 0L
    private var awaitingRoute = false
    var selectedDeviceId: Int? = null
        private set

    fun start(now: Long, previousDeviceId: Int? = null) {
        changedAt = now
        policy.changed(now)
        refresh(now)
        val initial = candidates.firstOrNull { it.id == previousDeviceId }
            ?: candidates.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_MIC }
        initial?.let { select(it, now) }
    }

    private fun refresh(now: Long) {
        if (refreshedAt != Long.MIN_VALUE && now - refreshedAt < 1000) return
        refreshedAt = now
        val devices = manager.getDevices(AudioManager.GET_DEVICES_INPUTS)
        // Android exposes multiple internal logical routes, not independent physical microphones.
        // Keep only one built-in route; don't switch the internal beamforming aliases.
        val builtIn = devices.filter { it.type == AudioDeviceInfo.TYPE_BUILTIN_MIC }.minByOrNull { it.id }
        candidates = listOfNotNull(builtIn) + devices.filter { it.type in EXTERNAL_TYPES }.sortedBy { it.id }
        rejected.retainAll(candidates.map { it.id }.toSet())
        if (requested != null && candidates.none { it.id == requested?.id }) select(null, now)
    }

    private fun select(device: AudioDeviceInfo?, now: Long): Boolean {
        if (!input.setPreferredDevice(device)) {
            device?.let { rejected.add(it.id) }
            return false
        }
        requested = device
        selectedDeviceId = null
        changedAt = now
        awaitingRoute = true
        policy.changed(now)
        return true
    }

    /** True means discard this frame and reset decoder history; never splice different routes. */
    fun observe(samples: ShortArray, count: Int, now: Long): Boolean {
        refresh(now)
        if (now - changedAt < MicrophoneScanPolicy.SETTLE_MS) return true
        if (awaitingRoute) {
            awaitingRoute = false
            val actual = input.routedDevice
            if (requested != null && actual?.id != requested?.id) {
                rejected.add(requested!!.id)
                select(null, now)
                return true
            }
            selectedDeviceId = actual?.id
            android.util.Log.i("DexMicrophone", "stage=route_verified requested=${requested?.id ?: 0} actual=${actual?.id ?: 0} type=${actual?.type ?: 0}")
            return true
        }
        val actual = input.routedDevice
        if (selectedDeviceId != actual?.id) {
            // System reroute/hotplug: reset before accepting samples from the new source.
            selectedDeviceId = actual?.id
            changedAt = now
            policy.changed(now)
            return true
        }
        if (candidates.size < 2 || !policy.observe(samples, count, now,
                input.activeRecordingConfiguration?.isClientSilenced == true)) return false
        val nextId = nextMicrophoneCandidate(candidates.map { it.id }, rejected, actual?.id) ?: return false
        val next = candidates.first { it.id == nextId }
        return select(next, now)
    }

    companion object {
        // Only attempt already-exposed Bluetooth input routes; never force global SCO/mode.
        private val EXTERNAL_TYPES = setOf(AudioDeviceInfo.TYPE_WIRED_HEADSET,
            AudioDeviceInfo.TYPE_USB_DEVICE, AudioDeviceInfo.TYPE_USB_HEADSET,
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO, AudioDeviceInfo.TYPE_BLE_HEADSET)
    }
}
