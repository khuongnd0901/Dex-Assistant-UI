package com.khuongnd.dexassistant.ui

import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.InputStream
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.security.MessageDigest
import java.util.Base64
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/** Offline RFC6455 fixture: no provider call, no claim of app launch verification. */
class CoordinatorProtocolDeviceTest {
    @Test fun correlatedPlanIsSerialDeduplicatedAndNotReplayedAfterReconnect() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val core = AssistantController.shared(context)
        val completed = CountDownLatch(1)
        val completionCount = AtomicInteger()
        var result: VoiceCommandResult? = null
        ProtocolFixture().use { server ->
            val connected = CountDownLatch(1)
            var facade: AssistantController? = null
            try {
                instrumentation.runOnMainSync {
                    core.disconnect()
                    facade = AssistantController(context) { if (it.status == "CONNECTED") connected.countDown() }
                    facade?.start()
                    facade?.provision("ws://127.0.0.1:${server.port}/ws/v1/assistant", "avd-test-token")
                }
                assertTrue(connected.await(10, TimeUnit.SECONDS))
                instrumentation.runOnMainSync { facade?.stop() }
                core.dispatchVoice("mở Chrome rồi mở YouTube") {
                    result = it; completionCount.incrementAndGet(); completed.countDown()
                }
                assertTrue("Fixture command timed out", completed.await(15, TimeUnit.SECONDS))
                assertEquals("DISPATCHED", result?.status)
                assertTrue("Reconnect observation timed out", server.finished.await(10, TimeUnit.SECONDS))
                server.failure?.let { throw AssertionError("Fixture failed", it) }
                assertEquals(listOf("first", "second"), server.actionIds)
                assertEquals(1, completionCount.get())
                assertEquals(1, server.commands.get())
            } finally {
                instrumentation.runOnMainSync { facade?.stop(); core.disconnect(); core.clearHistory() }
            }
        }
    }

    private class ProtocolFixture : AutoCloseable {
        private val server = ServerSocket(0)
        val port: Int get() = server.localPort
        val commands = AtomicInteger()
        val actionIds = mutableListOf<String>()
        val finished = CountDownLatch(1)
        @Volatile var failure: Throwable? = null
        private val worker = Thread {
            try {
                server.soTimeout = 10_000
                server.accept().use { socket ->
                    handshake(socket)
                    val input = socket.getInputStream()
                    val output = socket.getOutputStream()
                    var request: JSONObject
                    do { request = JSONObject(readFrame(input)) } while (request.getString("type") != "command.request")
                    commands.incrementAndGet()
                    val requestId = request.getString("messageId")
                    val actions = JSONArray()
                    actions.put(action("first", "com.android.chrome"))
                    actions.put(action("second", "com.google.android.youtube"))
                    val plan = envelope("execution.plan", requestId, JSONObject().put("planId", "fixture-plan").put("actions", actions))
                    writeFrame(output, plan)
                    writeFrame(output, plan) // A duplicate arriving during execution must not start a second sequence.
                    repeat(2) {
                        var frame: JSONObject
                        do { frame = JSONObject(readFrame(input)) } while (frame.getString("type") != "action.result")
                        val payload = frame.getJSONObject("payload")
                        check(payload.getString("status") == "SUCCESS") { "Actual action failure: $payload" }
                        actionIds.add(payload.getString("actionId"))
                    }
                    writeFrame(output, envelope("command.completed", requestId,
                        JSONObject().put("requestId", requestId).put("planId", "fixture-plan").put("status", "EXECUTED")))
                    // A transport reconnect must never submit the completed command again.
                }
                server.accept().use { socket ->
                    handshake(socket)
                    socket.soTimeout = 1500
                    try {
                        while (true) {
                            val frame = JSONObject(readFrame(socket.getInputStream()))
                            if (frame.getString("type") == "command.request") commands.incrementAndGet()
                        }
                    } catch (_: java.net.SocketTimeoutException) { }
                }
            } catch (error: Throwable) { failure = error }
            finally { finished.countDown() }
        }.apply { isDaemon = true; start() }

        private fun action(id: String, packageName: String) = JSONObject().put("actionId", id)
            .put("tool", "LAUNCH_APP").put("executionMode", "REMOTE")
            .put("arguments", JSONObject().put("packageName", packageName))

        private fun envelope(type: String, id: String, payload: JSONObject) = JSONObject()
            .put("version", 1).put("type", type).put("messageId", java.util.UUID.randomUUID().toString())
            .put("correlationId", id).put("sentAt", java.time.Instant.now().toString()).put("payload", payload).toString()

        private fun handshake(socket: Socket) {
            socket.soTimeout = 10_000
            val input = socket.getInputStream()
            val header = StringBuilder()
            while (!header.endsWith("\r\n\r\n")) {
                val byte = input.read(); check(byte >= 0); header.append(byte.toChar())
                check(header.length < 16_384)
            }
            val key = header.lines().first { it.startsWith("Sec-WebSocket-Key:", ignoreCase = true) }.substringAfter(':').trim()
            val digest = MessageDigest.getInstance("SHA-1").digest((key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11").toByteArray())
            val accept = Base64.getEncoder().encodeToString(digest)
            socket.getOutputStream().write(("HTTP/1.1 101 Switching Protocols\r\nUpgrade: websocket\r\nConnection: Upgrade\r\nSec-WebSocket-Accept: $accept\r\n\r\n").toByteArray())
            socket.getOutputStream().flush()
        }

        private fun readFrame(input: InputStream): String {
            val opcode = input.read(); check(opcode >= 0)
            val flags = input.read(); check(flags >= 0)
            var length = flags and 127
            if (length == 126) length = (input.read() shl 8) or input.read()
            check(length < 65_536 && length != 127)
            val mask = if (flags and 128 != 0) ByteArray(4).also { readFully(input, it) } else null
            val bytes = ByteArray(length); readFully(input, bytes)
            if (mask != null) bytes.indices.forEach { bytes[it] = (bytes[it].toInt() xor mask[it % 4].toInt()).toByte() }
            check(opcode and 15 == 1) { "Expected text frame" }
            return bytes.toString(Charsets.UTF_8)
        }

        private fun readFully(input: InputStream, bytes: ByteArray) {
            var offset = 0
            while (offset < bytes.size) { val count = input.read(bytes, offset, bytes.size - offset); check(count > 0); offset += count }
        }

        private fun writeFrame(output: OutputStream, text: String) {
            val bytes = text.toByteArray()
            output.write(0x81)
            if (bytes.size < 126) output.write(bytes.size)
            else { output.write(126); output.write(bytes.size shr 8); output.write(bytes.size and 255) }
            output.write(bytes); output.flush()
        }

        override fun close() { server.close(); worker.join(1000) }
    }
}
