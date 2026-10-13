package com.khuongnd.dexassistant.ui.voice

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.system.Os
import android.system.OsConstants
import android.system.ErrnoException
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicBoolean

/** Worker owns capture/release. Cancellation stops capture and closes pipe to unblock a stalled writer. */
internal class CommandMicrophoneSource(
    private val context: Context,
    private val preferredId: Int?,
    private val main: Handler,
    private val worker: Executor,
    private val ready: (ParcelFileDescriptor, Int) -> Unit,
    private val failed: (String) -> Unit,
) : AutoCloseable {
    private val stopped = AtomicBoolean()
    private val pipe = ParcelFileDescriptor.createPipe()
    private val lock = Any()
    private var input: AudioRecord? = null
    @Volatile private var readyAt = 0L
    fun recognizerReady() { readyAt = SystemClock.elapsedRealtime() }

    @SuppressLint("MissingPermission") // Service checks permission before entry; SecurityException is contained.
    fun start() { worker.execute {
        var audio: AudioRecord? = null
        var failure: String? = null
        try {
            if (stopped.get()) return@execute
            val devices = context.getSystemService(AudioManager::class.java).getDevices(AudioManager.GET_DEVICES_INPUTS)
            val device = devices.firstOrNull { it.id == preferredId }
                ?: devices.filter { it.type == AudioDeviceInfo.TYPE_BUILTIN_MIC }.minByOrNull { it.id }
            check(device != null) { "No microphone" }
            val minimum = AudioRecord.getMinBufferSize(16_000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
            check(minimum > 0)
            val record = AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, 16_000,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, minimum.coerceAtLeast(3200))
            audio = record
            check(record.state == AudioRecord.STATE_INITIALIZED)
            synchronized(lock) {
                if (stopped.get()) return@execute
                input = record
                check(record.setPreferredDevice(device))
                record.startRecording()
            }
            val samples = ShortArray(320)
            repeat(10) { check(record.read(samples, 0, samples.size) > 0) }
            if (stopped.get()) return@execute
            if (record.routedDevice?.id != device.id || record.activeRecordingConfiguration?.isClientSilenced == true) {
                failure = "STT_MIC_ROUTE_UNAVAILABLE"
                return@execute
            }
            android.util.Log.i("DexMicrophone", "stage=stt_source_verified requested=${device.id} actual=${record.routedDevice?.id ?: 0}")
            main.post { if (!stopped.get()) ready(pipe[0], device.type) }
            val bytes = ByteArray(640)
            val endpoint = PcmSpeechEndpoint()
            val captureStarted = SystemClock.elapsedRealtime()
            Os.fcntlInt(pipe[1].fileDescriptor, OsConstants.F_SETFL, OsConstants.O_NONBLOCK)
            run {
                while (!stopped.get()) {
                    val count = record.read(samples, 0, samples.size)
                    if (stopped.get()) break
                    check(count > 0)
                    if (record.routedDevice?.id != device.id || record.activeRecordingConfiguration?.isClientSilenced == true) {
                        failure = "STT_MIC_ROUTE_LOST"; break
                    }
                    for (i in 0 until count) {
                        bytes[2 * i] = samples[i].toByte()
                        bytes[2 * i + 1] = (samples[i].toInt() shr 8).toByte()
                    }
                    if (!writeFrame(bytes, count * 2)) {
                        if (!stopped.get()) failure = "STT_MIC_PIPE_TIMEOUT"
                        break
                    }
                    val now = SystemClock.elapsedRealtime()
                    if ((readyAt != 0L && endpoint.shouldEnd(samples, count, now - readyAt)) ||
                        now - captureStarted >= 25_000) break
                }
            }
        } catch (_: Exception) {
            if (!stopped.get()) failure = "STT_MIC_CAPTURE_FAILED"
        } finally {
            synchronized(lock) {
                runCatching { audio?.stop() }; audio?.release()
                if (input === audio) input = null
            }
            runCatching { pipe[1].close() }
            failure?.let { status -> main.post { if (!stopped.get()) failed(status) } }
        }
    } }

    private fun writeFrame(bytes: ByteArray, count: Int): Boolean {
        val deadline = SystemClock.elapsedRealtime() + 5000
        var offset = 0
        while (offset < count && !stopped.get()) {
            try {
                val written = Os.write(pipe[1].fileDescriptor, bytes, offset, count - offset)
                if (written > 0) offset += written
            } catch (error: ErrnoException) {
                if (error.errno != OsConstants.EAGAIN) throw error
                if (SystemClock.elapsedRealtime() >= deadline) return false
                Thread.sleep(5)
            }
        }
        return !stopped.get() && offset == count
    }

    override fun close() {
        if (!stopped.compareAndSet(false, true)) return
        // Close write/read endpoints before stop so pipe backpressure cannot retain the worker.
        pipe.forEach { runCatching { it.close() } }
        synchronized(lock) { runCatching { input?.stop() } }
    }
}
