package com.khuongnd.dexassistant.ui.history

import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** FIFO process-lifetime history worker. Closing drains accepted work, never cancels it. */
internal class HistoryWorkQueue(private val onCallbackFailure: () -> Unit = {}) : AutoCloseable {
    @Volatile private var worker: Thread? = null
    private val executor = Executors.newSingleThreadExecutor { work ->
        Thread(work, "DexHistory").apply { isDaemon = true; worker = this }
    }

    fun <T> enqueue(work: () -> T, complete: (Result<T>) -> Unit) {
        try {
            executor.execute { deliver(runCatching(work), complete) }
        } catch (rejected: java.util.concurrent.RejectedExecutionException) {
            deliver(Result.failure(rejected), complete)
        }
    }

    private fun <T> deliver(result: Result<T>, complete: (Result<T>) -> Unit) {
        // An observer failure must not terminate the Android process or lose later queued work.
        runCatching { complete(result) }.onFailure { runCatching(onCallbackFailure) }
    }

    fun <T> await(work: () -> T): T {
        if (Thread.currentThread() === worker) return work()
        return try { executor.submit<T> { work() }.get() }
        catch (failure: ExecutionException) { throw failure.cause ?: failure }
    }

    fun flush() = await { Unit }
    override fun close() { executor.shutdown() }
    fun awaitTermination(timeout: Long, unit: TimeUnit) = executor.awaitTermination(timeout, unit)
}

/** Main-thread-confined: superseded reads cannot restore a pre-clear snapshot. */
internal class HistoryPublicationGate {
    private var generation = 0L
    fun next(): Long = ++generation
    fun publish(ticket: Long, apply: () -> Unit) { if (ticket == generation) apply() }
}
