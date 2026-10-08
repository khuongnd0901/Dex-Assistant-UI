package com.khuongnd.dexassistant.ui

import android.Manifest
import android.app.ActivityOptions
import android.content.Intent
import android.hardware.display.DisplayManager
import android.app.UiAutomation
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.AudioAttributes
import android.media.MediaRecorder
import android.os.Build
import android.os.SystemClock
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.khuongnd.dexassistant.ui.screen.ScreenControlFixtureActivity
import com.khuongnd.dexassistant.ui.voice.VoiceAssistantService
import com.khuongnd.dexassistant.ui.voice.VoiceState
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.File
import org.json.JSONObject
import android.app.Application
import android.app.Activity
import android.os.Bundle
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.sqrt

/** Local recordings only. No Google provider, routing, playback, service/policy mutation. */
class SherpaRealCorpusCollectorTest {
    @Test fun collectConsentedRealSpeech() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val args = InstrumentationRegistry.getArguments()
        assumeTrue("Explicit local recording consent required", args.getString("recordLocalCorpusConsent") == "true")
        instrumentation.getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
        val context = instrumentation.targetContext
        require(Build.MODEL.startsWith("SM-F926") && Build.VERSION.SDK_INT >= 29)
        require(context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
        require(VoiceAssistantService.active?.currentVoiceState() == VoiceState.PAUSED) {
            "Owner must pause production voice before opening collector; no policy is changed automatically"
        }
        val directory = File(context.filesDir, "sherpa-vi-real-corpus/session-${System.currentTimeMillis()}")
        require(directory.mkdirs())
        File(directory, "collection.json").writeText("""{"task":"T-017","consent":"local_audio_and_transcripts","purpose":"12_prompt_pilot_not_200_coverage","speechEndMetric":"NOT_MEASURED","capture":"PCM16_mono_16000","providerUpload":false}""")
        val prompts = listOf("Bật wifi", "Tắt wifi", "Tăng âm lượng", "Giảm âm lượng", "Đặt âm lượng năm mươi phần trăm", "Mở YouTube", "Tạm dừng nhạc", "Tiếp tục nhạc", "Mở bản đồ", "Tìm địa điểm hồ hoàn kiếm", "Cuộn xuống", "Cuộn lên")
        val groups = listOf("system", "system", "system", "system", "system", "dex_ui", "media", "media", "dex_ui", "maps", "dex_ui", "dex_ui")
        val finished = CountDownLatch(1)
        val capturing = AtomicBoolean(false)
        val cancelled = AtomicBoolean(false)
        val lock = Any()
        var worker: Thread? = null
        var recorder: AudioRecord? = null
        var playback: AudioTrack? = null
        var pending: ByteArray? = null
        var pendingMetadata: JSONObject? = null
        var index = 0
        var played = false
        val accepted = mutableListOf<Pair<String, String>>()
        val lifecycle = object : Application.ActivityLifecycleCallbacks {
            override fun onActivityDestroyed(activity: Activity) {
                if (activity is ScreenControlFixtureActivity) {
                    cancelled.set(true); capturing.set(false)
                    synchronized(lock) { try { recorder?.stop() } catch (_: Exception) { } }
                    finished.countDown()
                }
            }
            override fun onActivityCreated(a: Activity, b: Bundle?) = Unit
            override fun onActivityStarted(a: Activity) = Unit
            override fun onActivityResumed(a: Activity) = Unit
            override fun onActivityPaused(a: Activity) = Unit
            override fun onActivityStopped(a: Activity) = Unit
            override fun onActivitySaveInstanceState(a: Activity, b: Bundle) = Unit
        }
        val application = context.applicationContext as Application
        application.registerActivityLifecycleCallbacks(lifecycle)
        try {
            val displayId = args.getString("collectorDisplayId")?.toIntOrNull()
            require(args.getString("collectorDisplayId") == null || displayId != null)
            val options = ActivityOptions.makeBasic()
            if (displayId != null) {
                require(context.getSystemService(DisplayManager::class.java).getDisplay(displayId) != null)
                options.launchDisplayId = displayId
            }
            ActivityScenario.launch<ScreenControlFixtureActivity>(Intent(context, ScreenControlFixtureActivity::class.java), options.toBundle()).use { scenario ->
                scenario.onActivity { activity ->
                    val layout = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL; setPadding(24, 24, 24, 24) }
                    val status = TextView(activity)
                    val phrase = TextView(activity).apply { textSize = 24f }
                    val reference = EditText(activity).apply { hint = "Ghi đúng câu bạn thực sự nói sau khi nghe lại" }
                    val review = android.widget.CheckBox(activity).apply { text = "Tôi đã nghe lại và sửa reference đúng câu thực sự nói" }
                    fun showPrompt() { phrase.text = "${index + 1}/${prompts.size}: ${prompts[index]}"; review.isChecked = false; reference.setText(""); played = false }
                    val recordRow = LinearLayout(activity).apply { orientation = LinearLayout.HORIZONTAL }
                    val reviewRow = LinearLayout(activity).apply { orientation = LinearLayout.HORIZONTAL }
                    fun button(label: String, action: () -> Unit) = Button(activity).apply {
                        text = label.substringBefore(" / ")
                        setOnClickListener { action() }
                        val row = when {
                            label.startsWith("Ghi") || label.startsWith("Dừng") || label.startsWith("Nghe") -> recordRow
                            label.startsWith("Xác nhận") || label.startsWith("Thử") || label.startsWith("Bỏ") -> reviewRow
                            else -> null
                        }
                        if (row == null) layout.addView(this)
                        else row.addView(this, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
                    }
                    layout.addView(TextView(activity).apply { text = "T-017: ghi âm cục bộ có đồng ý. Không thực thi lệnh. Chỉ bắt đầu khi bạn bấm Ghi. Nói đúng câu, bấm Dừng, sau đó Xác nhận hoặc Thử lại. Tối đa 15 giây. Giữ trợ lý ở Tạm dừng." })
                    layout.addView(phrase); layout.addView(status); layout.addView(reference); layout.addView(review); layout.addView(recordRow); layout.addView(reviewRow); showPrompt()
                    button("Ghi / Record") {
                        if (index >= prompts.size || cancelled.get() || worker?.isAlive == true || pending != null || capturing.get()) return@button
                        if (VoiceAssistantService.active?.currentVoiceState() != VoiceState.PAUSED) { status.text = "Hãy Tạm dừng trợ lý trước khi ghi."; return@button }
                        playback?.release(); playback = null; review.isChecked = false; reference.setText(""); played = false; capturing.set(true); status.text = "Đang ghi — nói câu hiển thị, rồi bấm Dừng."
                        worker = Thread {
                            var local: AudioRecord? = null
                            try {
                                val size = maxOf(3200, AudioRecord.getMinBufferSize(16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT))
                                local = AudioRecord(MediaRecorder.AudioSource.MIC, 16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, size * 2)
                                synchronized(lock) { recorder = local }
                                require(local.state == AudioRecord.STATE_INITIALIZED)
                                local.startRecording()
                                require(local.recordingState == AudioRecord.RECORDSTATE_RECORDING)
                                val bytes = ByteArrayOutputStream(); val buffer = ByteArray(size)
                                val start = SystemClock.elapsedRealtime(); var silenced = false; var configObserved = false; var configuration: android.media.AudioRecordingConfiguration? = null
                                while (capturing.get() && !cancelled.get() && SystemClock.elapsedRealtime() - start < 15000 && bytes.size() < 480000) {
                                    if (VoiceAssistantService.active?.currentVoiceState() != VoiceState.PAUSED) throw IllegalStateException("VOICE_NOT_PAUSED")
                                    local.activeRecordingConfiguration?.let { configuration = it; configObserved = true; silenced = silenced || it.isClientSilenced }
                                    val count = local.read(buffer, 0, minOf(buffer.size, 480000 - bytes.size()), AudioRecord.READ_BLOCKING)
                                    require(count > 0) { "AUDIO_READ_FAILED" }; bytes.write(buffer, 0, count)
                                }
                                val pcm = bytes.toByteArray()
                                require(configObserved && !silenced && pcm.size >= 16000 && pcm.size % 2 == 0) { "SHORT_OR_SILENCED_AUDIO" }
                                require(SherpaCorpusPcm.usable(pcm)) { "NO_USABLE_AUDIO_ENERGY" }
                                if (!cancelled.get()) {
                                    pendingMetadata = SherpaCorpusPcm.metadata(pcm).apply {
                                        put("requestedSampleRate", 16000); put("clientSampleRate", configuration?.clientFormat?.sampleRate)
                                        put("deviceSampleRate", configuration?.format?.sampleRate); put("clientAudioSource", configuration?.clientAudioSource)
                                        put("requestedAudioSource", "MIC"); put("audioSessionId", local.audioSessionId)
                                        put("routeDeviceType", configuration?.audioDevice?.type); put("routeDeviceId", configuration?.audioDevice?.id)
                                        put("configObserved", configObserved); put("clientSilenced", silenced)
                                        put("speechEndMetric", "NOT_MEASURED; stop tap is not acoustic end")
                                    }
                                    pending = pcm; activity.runOnUiThread { if (!activity.isDestroyed && !activity.isFinishing) status.text = "Đã thu. Bạn đã nói đúng câu hiển thị? Bấm Xác nhận; nếu khác, Thử lại." } }
                            } catch (_: Exception) {
                                if (!cancelled.get()) activity.runOnUiThread { if (!activity.isDestroyed && !activity.isFinishing) status.text = "Bản ghi không hợp lệ hoặc micro xung đột. Giữ trợ lý Tạm dừng rồi Thử lại." }
                            } finally {
                                capturing.set(false)
                                try { if (local?.recordingState == AudioRecord.RECORDSTATE_RECORDING) local.stop() } catch (_: Exception) { }
                                local?.release(); synchronized(lock) { recorder = null }
                            }
                        }.apply { name = "T017-local-recording"; start() }
                    }
                    button("Dừng / Stop") { capturing.set(false) }
                    button("Nghe lại / Playback") {
                        val pcm = pending ?: return@button
                        if (VoiceAssistantService.active?.currentVoiceState() != VoiceState.PAUSED) { status.text = "Hãy Tạm dừng trợ lý trước khi nghe lại."; return@button }
                        if (worker?.isAlive == true || capturing.get()) return@button
                        playback?.release()
                        playback = AudioTrack.Builder()
                            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                            .setAudioFormat(AudioFormat.Builder().setSampleRate(16000).setEncoding(AudioFormat.ENCODING_PCM_16BIT).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                            .setTransferMode(AudioTrack.MODE_STATIC).setBufferSizeInBytes(pcm.size).build().also { track ->
                                track.write(pcm, 0, pcm.size); track.play(); played = true
                            }
                        if (reference.text.isBlank()) reference.setText(prompts[index])
                        status.text = "Nghe lại và sửa câu gợi ý nếu bạn nói khác. Đánh dấu xác nhận rồi bấm Xác nhận."
                    }
                    button("Xác nhận / Confirm") {
                        if (index >= prompts.size || cancelled.get()) return@button
                        val pcm = pending ?: return@button
                        if (worker?.isAlive == true) return@button
                        val actualReference = reference.text.toString().trim()
                        if (!played || !review.isChecked || actualReference.isBlank() || actualReference.any { it == ',' || it == '\n' || it == '\r' }) {
                            status.text = "Cần nghe lại, nhập reference và đánh dấu xác nhận (không dấu phẩy/xuống dòng)."; return@button
                        }
                        playback?.release(); playback = null
                        val id = "sample%03d".format(index + 1)
                        writeWave(File(directory, "$id.wav"), pcm)
                        File(directory, "$id.metadata.json").writeText(requireNotNull(pendingMetadata).put("sampleId", id).toString(2))
                        pendingMetadata = null
                        accepted.add(id to actualReference); pending = null
                        File(directory, "references.csv").writeText("sampleId,reference,group,consent,source\n" + accepted.joinToString("\n") { "${it.first},${it.second},${groups[it.first.removePrefix("sample").toInt() - 1]},yes,human_recording" } + "\n")
                        index++
                        if (index == prompts.size) { status.text = "Hoàn tất 12 câu. Corpus vẫn thiếu coverage200. Không so sánh Google."; finished.countDown() } else { showPrompt(); status.text = "Sẵn sàng cho câu tiếp theo." }
                    }
                    button("Thử lại / Retry") { if (worker?.isAlive != true) { pending = null; playback?.release(); playback = null; review.isChecked = false; reference.setText(""); status.text = "Bấm Ghi để thử lại." } }
                    button("Bỏ qua / Skip") { if (index < prompts.size && !cancelled.get() && worker?.isAlive != true) { pending = null; playback?.release(); playback = null; index++; if (index == prompts.size) finished.countDown() else showPrompt() } }
                    val cancelButton = button("Hủy / Cancel") { cancelled.set(true); capturing.set(false); pending = null; finished.countDown() }
                    activity.setContentView(android.widget.ScrollView(activity).apply { addView(layout) })
                    if (args.getString("collectorUiSmoke") == "true") {
                        require(!capturing.get() && recorder == null && pending == null)
                        require(recordRow.childCount == 3 && reviewRow.childCount == 3 && cancelButton.isEnabled)
                        cancelButton.performClick()
                    }
                }
                instrumentation.sendStatus(0, android.os.Bundle().apply { putString("localCorpusDirectory", directory.absolutePath); putString("collector", "WAITING_FOR_EXPLICIT_USER_RECORD_TAP") })
                val completed = finished.await(20, TimeUnit.MINUTES)
                val outcome = if (!completed) "TIMEOUT" else if (cancelled.get()) "CANCELLED" else if (accepted.isEmpty()) "NO_VALID_AUDIO" else "PILOT_COMPLETE"
                instrumentation.sendStatus(0, Bundle().apply { putString("collectorOutcome", outcome); putInt("acceptedAudioCount", accepted.size); putBoolean("microphoneOpened", worker != null) })
                require(completed) { "Collector timed out; no acceptance claim" }
                require(cancelled.get() || accepted.isNotEmpty()) { "No valid corpus; all prompts skipped" }
                cancelled.set(true); capturing.set(false)
                synchronized(lock) { try { recorder?.stop() } catch (_: Exception) { } }
                worker?.join(5000)
            }
        } finally {
            cancelled.set(true); capturing.set(false)
            synchronized(lock) { try { recorder?.stop() } catch (_: Exception) { } }
            worker?.join(5000)
            pending = null; pendingMetadata = null
            application.unregisterActivityLifecycleCallbacks(lifecycle)
            playback?.release(); playback = null
            // Accepted files retained locally only; unconfirmed/cancelled PCM never written.
        }
    }

    private fun writeWave(file: File, pcm: ByteArray) {
        file.outputStream().use { it.write(SherpaCorpusPcm.wave(pcm)) }
    }
}

internal object SherpaCorpusPcm {
    fun usable(pcm: ByteArray): Boolean {
        if (pcm.size !in 16000..480000 || pcm.size % 2 != 0) return false
        val samples = ByteBuffer.wrap(pcm).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
        var energy = 0.0; var peak = 0
        while (samples.hasRemaining()) { val value = samples.get().toInt(); energy += value.toDouble() * value; peak = maxOf(peak, kotlin.math.abs(value)) }
        return sqrt(energy / (pcm.size / 2)) >= 40 && peak >= 200
    }
    fun metadata(pcm: ByteArray): JSONObject {
        val samples = ByteBuffer.wrap(pcm).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
        var energy = 0.0; var clipped = 0
        while (samples.hasRemaining()) { val value = samples.get().toInt(); energy += value.toDouble() * value; if (kotlin.math.abs(value) >= 32760) clipped++ }
        return JSONObject().put("audioSeconds", pcm.size / 32000.0).put("rmsPcm16", sqrt(energy / (pcm.size / 2)))
            .put("clippedFraction", clipped.toDouble() / (pcm.size / 2))
    }
    fun wave(pcm: ByteArray): ByteArray {
        require(pcm.size in 16000..480000 && pcm.size % 2 == 0)
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
        header.put("RIFF".toByteArray()).putInt(36 + pcm.size).put("WAVEfmt ".toByteArray())
            .putInt(16).putShort(1).putShort(1).putInt(16000).putInt(32000).putShort(2).putShort(16)
            .put("data".toByteArray()).putInt(pcm.size)
        return header.array() + pcm
    }
}
