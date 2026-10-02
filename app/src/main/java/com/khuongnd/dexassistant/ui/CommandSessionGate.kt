package com.khuongnd.dexassistant.ui

/** Main-thread command admission and correlation; old callbacks cannot release a new command. */
internal class CommandSessionGate {
    var currentId: String? = null
        private set

    private val queued = mutableSetOf<String>()
    @Synchronized fun enqueue(requestId: String) { queued.add(requestId) }
    @Synchronized fun cancelQueued(requestId: String) { queued.remove(requestId) }
    @Synchronized fun beginQueued(requestId: String): Boolean? {
        if (!queued.remove(requestId)) return null
        return begin(requestId)
    }
    @Synchronized fun begin(requestId: String): Boolean {
        if (currentId != null) return false
        currentId = requestId
        return true
    }

    @Synchronized fun finish(requestId: String): Boolean {
        if (currentId != requestId) return false
        currentId = null
        return true
    }
}
