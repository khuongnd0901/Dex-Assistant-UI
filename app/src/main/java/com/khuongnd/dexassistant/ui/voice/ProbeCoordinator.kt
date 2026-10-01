package com.khuongnd.dexassistant.ui.voice

/** Invalidates pending probe completion after pause/stop and suppresses duplicate starts. */
class ProbeCoordinator {
    private var generation = 0
    private var pending = false

    @Synchronized fun begin(): Int? {
        if (pending) return null
        pending = true
        return ++generation
    }

    @Synchronized fun isCurrent(token: Int) = pending && token == generation

    @Synchronized fun complete(token: Int): Boolean {
        if (!isCurrent(token)) return false
        pending = false
        return true
    }

    @Synchronized fun cancel() {
        generation++
        pending = false
    }
}
