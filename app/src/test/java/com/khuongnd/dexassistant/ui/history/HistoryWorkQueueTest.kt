package com.khuongnd.dexassistant.ui.history

import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class HistoryWorkQueueTest {
    @Test fun callbackExceptionIsReportedAndDoesNotLoseLaterWork() {
        var callbackFailed = false
        HistoryWorkQueue { callbackFailed = true }.use { queue ->
            queue.enqueue({ 1 }) { error("observer failed") }
            assertEquals(2, queue.await { 2 })
            assertTrue(callbackFailed)
        }
    }
    @Test fun immutableEntryIsRedactedBeforeTheBlockedQueueReceivesIt() {
        val safe = HistoryPrivacy.prepare("mật khẩu nhiều từ bí mật", "LOCAL", null, "password another secret", 123)
        assertEquals("[REDACTED]", safe.originalText)
        assertEquals("[REDACTED]", safe.outcome)
        HistoryWorkQueue().use { queue ->
            val entered = CountDownLatch(1); val release = CountDownLatch(1)
            queue.enqueue({ entered.countDown(); check(release.await(3, TimeUnit.SECONDS)) }) { }
            assertTrue(entered.await(3, TimeUnit.SECONDS))
            var received: HistoryEntry? = null
            queue.enqueue({ safe }) { received = it.getOrThrow() }
            assertNull(received)
            release.countDown(); queue.flush()
            assertSame(safe, received)
            assertEquals("[REDACTED]", received?.originalText)
        }
    }
    @Test fun fifoBarrierAndFailureKeepLaterWorkAlive() {
        HistoryWorkQueue().use { queue ->
            val entered = CountDownLatch(1); val release = CountDownLatch(1)
            val events = mutableListOf<String>()
            queue.enqueue({ entered.countDown(); check(release.await(3, TimeUnit.SECONDS)); events.add("record") }) { }
            assertTrue(entered.await(3, TimeUnit.SECONDS))
            queue.enqueue<Int>({ error("storage unavailable") }) { assertTrue(it.isFailure); events.add("failure") }
            queue.enqueue({ events.add("clear") }) { }
            queue.enqueue({ events.add("later-record") }) { }
            release.countDown()
            queue.flush()
            assertEquals(listOf("record", "failure", "clear", "later-record"), events)
            assertEquals(4, queue.await { events.size })
        }
    }

    @Test fun shutdownDrainsAcceptedWorkAndRejectsNewWorkTruthfully() {
        val queue = HistoryWorkQueue()
        val entered = CountDownLatch(1); val release = CountDownLatch(1)
        var completed = false; var rejected = false
        queue.enqueue({ entered.countDown(); check(release.await(3, TimeUnit.SECONDS)); 1 }) { completed = it.isSuccess }
        assertTrue(entered.await(3, TimeUnit.SECONDS))
        queue.close()
        queue.enqueue({ 2 }) { rejected = it.isFailure }
        assertTrue(rejected)
        release.countDown()
        assertTrue(queue.awaitTermination(3, TimeUnit.SECONDS)); assertTrue(completed)
    }

    @Test fun newerPublicationAndClearRejectOldSnapshots() {
        val gate = HistoryPublicationGate()
        val oldRead = gate.next()
        val clear = gate.next()
        var snapshot = listOf("newer")
        gate.publish(oldRead) { snapshot = listOf("old") }
        gate.publish(clear) { snapshot = emptyList() }
        gate.publish(oldRead) { snapshot = listOf("resurrected") }
        assertTrue(snapshot.isEmpty())
    }
}
