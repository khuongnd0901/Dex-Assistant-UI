package com.khuongnd.dexassistant.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.testTag
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.khuongnd.dexassistant.ui.theme.DexAssistantTheme
import com.khuongnd.dexassistant.ui.protocol.QuotaFreshness

class MainActivity : ComponentActivity() {
    private var uiState by mutableStateOf(UiState())
    private lateinit var controller: AssistantController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        controller = AssistantController(applicationContext) { uiState = it }
        controller.start()
        setContent {
            DexAssistantTheme {
                AssistantScreen(uiState, controller::provision, controller::connect,
                    controller::refreshHealth, controller::applyPermissions,
                    controller::startVoice, controller::stopVoice, controller::send, controller::clearHistory,
                    onPauseVoice = controller::pauseVoice, onResumeVoice = controller::resumeVoice, onDiagnostic = controller::diagnosticListen)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        controller.observeVoiceHealth()
        controller.refreshHealth()
        controller.autoStartVoice()
    }

    override fun onStop() {
        controller.stopObservingVoiceHealth()
        super.onStop()
    }

    override fun onDestroy() {
        controller.stop()
        super.onDestroy()
    }
}

@androidx.compose.runtime.Composable
fun AssistantScreen(
    state: UiState, onProvision: (String, String) -> Unit, onConnect: () -> Unit,
    onRefresh: () -> Unit, onApplyPermissions: () -> Unit, onStartVoice: () -> Unit,
    onStopVoice: () -> Unit, onSend: (String) -> Unit, onClearHistory: () -> Unit,
    modifier: Modifier = Modifier, onPauseVoice: () -> Unit = {}, onResumeVoice: () -> Unit = {}, onDiagnostic: () -> Unit = {},
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val live by com.khuongnd.dexassistant.ui.voice.VoiceLiveState.state.collectAsState()
    var dialog by androidx.compose.runtime.remember { mutableStateOf("") }
    var detail by androidx.compose.runtime.remember { mutableStateOf("") }
    var endpoint by rememberSaveable(state.endpoint) { mutableStateOf(state.endpoint) }
    // Credentials, typed commands and transcripts are never saved in the instance-state bundle.
    var token by androidx.compose.runtime.remember { mutableStateOf("") }
    var command by androidx.compose.runtime.remember { mutableStateOf("") }
    Scaffold(modifier = modifier.fillMaxSize().testTag("dashboard")) { inset ->
        androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize().padding(inset).padding(8.dp)) {
            val wide = maxWidth >= 700.dp
            val compact = maxHeight < 400.dp
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (!compact) Text("DeX Assistant · Hey Hey", style = MaterialTheme.typography.titleMedium)
                Text("${state.voiceState} · ${state.dexStatus} · ${state.accessibilityStatus}", maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.clickable { detail = "${state.voiceDetail}\n${state.status}: ${state.message}\n${state.assistantReadiness}\n${state.dexStatus}\n${state.accessibilityStatus}\n${live.microphone}"; dialog = "detail" })
                @androidx.compose.runtime.Composable fun Controls() {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Button(onClick = if (state.voiceState == "PAUSED") onResumeVoice else onStartVoice, enabled = state.voiceState in setOf("STOPPED", "PAUSED", "ERROR"), modifier = Modifier.weight(1f).testTag("voiceStart"), contentPadding = androidx.compose.foundation.layout.PaddingValues(4.dp)) { Text(if (state.voiceState == "PAUSED") "Tiếp tục" else if (state.voiceState in setOf("STOPPED", "ERROR")) "Bắt đầu" else "Tự nghe", maxLines = 1) }
                            Button(onClick = onPauseVoice, modifier = Modifier.weight(1f).testTag("voicePause"), contentPadding = androidx.compose.foundation.layout.PaddingValues(4.dp)) { Text("Tạm dừng", maxLines = 1) }
                            Button(onClick = onStopVoice, modifier = Modifier.weight(1f).testTag("voiceStop"), contentPadding = androidx.compose.foundation.layout.PaddingValues(4.dp)) { Text("Dừng", maxLines = 1) }
                        }
                        Button(onClick = onDiagnostic, Modifier.fillMaxWidth().testTag("diagnosticListen")) { Text("Nghe thử · không thực hiện lệnh", maxLines = 1) }
                        if (!compact) Text(live.microphone, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            modifier = Modifier.clickable { detail = live.microphone; dialog = "detail" })
                    }
                }
                @androidx.compose.runtime.Composable fun Transcript(modifier: Modifier) {
                    androidx.compose.material3.Card(modifier.testTag("transcript").clickable(enabled = live.transcript.isNotBlank()) { detail = live.transcript; dialog = "detail" }) {
                        Column(Modifier.fillMaxSize().padding(8.dp)) {
                            if (!compact) Text(if (live.diagnostic) "Nghe thử" else if (live.final) "Đã nghe" else "Đang nghe", style = MaterialTheme.typography.labelLarge)
                            Text(live.transcript.ifBlank { if (live.ready) "Sẵn sàng — nói lệnh." else "Nói Hey Hey, hoặc chọn Nghe thử." }, Modifier.weight(1f).testTag("transcriptText"), maxLines = if (compact) 1 else 8,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                            if (!compact) androidx.compose.material3.TextButton(onClick = { detail = live.transcript; dialog = "detail" }, enabled = live.transcript.isNotBlank()) { Text("Xem toàn bộ") }
                        }
                    }
                }
                if (wide) androidx.compose.foundation.layout.Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.weight(1f)) { Controls() }
                    Transcript(Modifier.weight(1f).fillMaxSize())
                } else {
                    Controls()
                    Transcript(Modifier.weight(1f).fillMaxWidth())
                }
                androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("Cài đặt" to "settings", "Quota" to "quota", "Lịch sử" to "history", "Lệnh" to "command").forEach { (label, key) ->
                        androidx.compose.material3.OutlinedButton(onClick = { dialog = key }, Modifier.weight(1f).testTag(key), contentPadding = androidx.compose.foundation.layout.PaddingValues(4.dp)) { Text(label, maxLines = 1) }
                    }
                }
            }
        }
    }
    if (dialog.isNotBlank()) androidx.compose.material3.AlertDialog(
        onDismissRequest = { dialog = ""; token = ""; command = "" },
        confirmButton = { androidx.compose.material3.TextButton(onClick = { dialog = ""; token = ""; command = "" }) { Text("Đóng") } },
        title = { Text(when (dialog) { "settings" -> "Cài đặt"; "quota" -> "AI quota"; "history" -> "Lịch sử"; "command" -> "Lệnh nhập tay"; else -> "Chi tiết" }) },
        text = {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when (dialog) {
                    "settings" -> {
                        OutlinedTextField(endpoint, { endpoint = it }, label = { Text("WebSocket endpoint") }, singleLine = true)
                        OutlinedTextField(token, { token = it }, label = { Text("Bearer token") }, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), singleLine = true)
                        Button(onClick = { onProvision(endpoint.trim(), token); token = "" }, enabled = endpoint.isNotBlank() && token.isNotBlank()) { Text("Provision and connect") }
                        Button(onClick = onConnect, enabled = state.endpoint.isNotBlank()) { Text("Connect") }
                        Button(onClick = onRefresh) { Text("Refresh health") }
                        Button(onClick = onApplyPermissions) { Text("Apply permission profile") }
                        Button(onClick = { context.startActivity(android.content.Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)) }) { Text("Trợ năng DeX") }
                        Text("Bật công tắc dịch vụ DeX Assistant để điều khiển số. Phím tắt Trợ năng có thể bật/tắt dịch vụ; chỉ giữ phím tắt nếu bạn muốn dùng nó. Ứng dụng hiển thị trạng thái thật và không tự bật dịch vụ.")
                        Text("Device Owner: ${state.deviceOwner}\n${state.assistantReadiness}\n${state.voiceDetail}")
                        state.capabilities.forEach { (key, value) -> Text("$key: $value") }
                    }
                    "quota" -> {
                        val quota = state.quota
                        if (quota == null) Text("Quota: unknown") else {
                            Text(if (state.quotaStale || QuotaFreshness.isStale(quota.staleAfter)) "Stale" else "Fresh")
                            quota.providers.forEach { provider -> Text("${provider.providerAlias}/${provider.modelAlias}: ${provider.state}"); provider.budgets.forEach { Text("${it.unit}: ${it.used}/${it.limit}, remaining ${it.remaining}") } }
                        }
                    }
                    "history" -> { state.history.forEach { Text("${it.route}: ${it.originalText} — ${it.outcome}") }; Button(onClick = onClearHistory, enabled = state.history.isNotEmpty()) { Text("Clear command history") } }
                    "command" -> { OutlinedTextField(command, { command = it }, label = { Text("Vietnamese command") }); Button(onClick = { onSend(command); command = ""; dialog = "" }, enabled = command.isNotBlank()) { Text("Run command") } }
                    else -> Text(detail)
                }
            }
        },
    )
}
