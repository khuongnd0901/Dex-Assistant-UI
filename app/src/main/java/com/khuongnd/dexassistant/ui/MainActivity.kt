package com.khuongnd.dexassistant.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.khuongnd.dexassistant.ui.theme.DexAssistantTheme
import com.khuongnd.dexassistant.ui.protocol.QuotaFreshness
import com.khuongnd.dexassistant.ui.voice.VoiceAssistantService

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
                    onPauseVoice = controller::pauseVoice, onResumeVoice = controller::resumeVoice, onDiagnostic = controller::diagnosticListen,
                    onEndChat = { if (VoiceAssistantService.active != null) startService(android.content.Intent(this, VoiceAssistantService::class.java).setAction(VoiceAssistantService.ACTION_END_CHAT)) })
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
    modifier: Modifier = Modifier, onPauseVoice: () -> Unit = {}, onResumeVoice: () -> Unit = {}, onDiagnostic: () -> Unit = {}, onEndChat: () -> Unit = {},
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val live by com.khuongnd.dexassistant.ui.voice.VoiceLiveState.state.collectAsState()
    var dialog by androidx.compose.runtime.remember { mutableStateOf("") }
    var detail by androidx.compose.runtime.remember { mutableStateOf("") }
    var endpoint by rememberSaveable(state.endpoint) { mutableStateOf(state.endpoint) }
    // Credentials, typed commands and transcripts are never saved in the instance-state bundle.
    var token by androidx.compose.runtime.remember { mutableStateOf("") }
    var command by androidx.compose.runtime.remember { mutableStateOf("") }
    val localAliases = androidx.compose.runtime.remember(context) { com.khuongnd.dexassistant.ui.localdata.LocalAliases(context) }
    var homeAddress by androidx.compose.runtime.remember { mutableStateOf(localAliases.savedPlace("home")) }
    var workAddress by androidx.compose.runtime.remember { mutableStateOf(localAliases.savedPlace("work")) }
    var contactAlias by androidx.compose.runtime.remember { mutableStateOf("") }
    var contactName by androidx.compose.runtime.remember { mutableStateOf("") }
    var aliasFeedback by androidx.compose.runtime.remember { mutableStateOf("") }
    val contactPermissions = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()) { result ->
        aliasFeedback = if (result[android.Manifest.permission.READ_CONTACTS] == true && result[android.Manifest.permission.CALL_PHONE] == true) "Đã cấp quyền danh bạ và gọi điện." else "Chỉ dùng quyền đã cấp; thiếu quyền gọi sẽ mở bàn phím gọi."
    }
    val chatActive by com.khuongnd.dexassistant.ui.voice.VoiceLiveState.chatActive.collectAsState()
    val examples = androidx.compose.runtime.remember(context) { LocalCommandExamples.load(context) }
    var showAllCommands by androidx.compose.runtime.remember { mutableStateOf(false) }
    var commandSearch by androidx.compose.runtime.remember { mutableStateOf("") }
    var expandedAliases by androidx.compose.runtime.remember { mutableStateOf(emptySet<String>()) }
    var advancedExpanded by androidx.compose.runtime.remember { mutableStateOf(false) }
    val canRun = command.isNotBlank() && !Regex("\\{[^}]+\\}").containsMatchIn(command)
    @androidx.compose.runtime.Composable fun Controls() {
        androidx.compose.foundation.layout.Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Button(onClick = if (state.voiceState == "PAUSED") onResumeVoice else onStartVoice,
                enabled = state.voiceState in setOf("STOPPED", "PAUSED", "ERROR"),
                modifier = Modifier.weight(1f).testTag("voiceStart"), contentPadding = androidx.compose.foundation.layout.PaddingValues(4.dp)) {
                Text(if (state.voiceState == "PAUSED") "Tiếp tục" else if (state.voiceState in setOf("STOPPED", "ERROR")) "Bắt đầu" else "Tự nghe", maxLines = 1)
            }
            Button(onClick = onPauseVoice, modifier = Modifier.weight(1f).testTag("voicePause"), contentPadding = androidx.compose.foundation.layout.PaddingValues(4.dp)) { Text("Tạm dừng", maxLines = 1) }
            Button(onClick = onStopVoice, modifier = Modifier.weight(1f).testTag("voiceStop"), contentPadding = androidx.compose.foundation.layout.PaddingValues(4.dp)) { Text("Dừng", maxLines = 1) }
        }
    }
    @androidx.compose.runtime.Composable fun Transcript(modifier: Modifier) {
        androidx.compose.material3.Card(modifier.testTag("transcript").clickable(enabled = live.transcript.isNotBlank()) { detail = live.transcript; dialog = "detail" }) {
            Column(Modifier.fillMaxSize().padding(8.dp)) {
                Text(if (live.diagnostic) "Nghe thử" else if (live.final) "Đã nghe" else "Đang nghe", style = MaterialTheme.typography.labelLarge)
                Text(live.transcript.ifBlank { if (live.ready) "Sẵn sàng — nói lệnh." else "Nói Hey Hey để ra lệnh." },
                    Modifier.weight(1f).testTag("transcriptText"), maxLines = 8, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            }
        }
    }
    @androidx.compose.runtime.Composable fun CommandPane(modifier: Modifier, compactFull: Boolean) {
        Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (!compactFull) androidx.compose.foundation.layout.Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedTextField(command, { command = it }, modifier = Modifier.weight(1f).testTag("commandInput"),
                    label = { Text("Nhập lệnh") }, singleLine = true)
                Button(onClick = { onSend(command.trim()); command = "" }, enabled = canRun,
                    modifier = Modifier.testTag("runCommand")) { Text("Chạy") }
            }
            androidx.compose.foundation.layout.Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Lệnh local", style = MaterialTheme.typography.titleSmall)
                androidx.compose.material3.TextButton(onClick = { showAllCommands = !showAllCommands }, Modifier.testTag("showAllCommands")) {
                    Text(if (showAllCommands) "Thu gọn" else "Xem tất cả")
                }
            }
            Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).testTag("commandList"),
                verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (showAllCommands) OutlinedTextField(commandSearch, { commandSearch = it },
                    modifier = Modifier.fillMaxWidth().testTag("commandSearch"), label = { Text("Tìm lệnh hoặc nhóm") }, singleLine = true)
                if (compactFull) Text("Chọn câu để điền ô lệnh.", style = MaterialTheme.typography.bodySmall)
                val visible = if (showAllCommands) LocalCommandExamples.search(examples, commandSearch)
                    else examples.filter { it.id in LocalCommandExamples.featuredIds }
                if (showAllCommands) Text("${examples.size} lệnh · ${examples.sumOf { it.phrases.size }} cách nói", style = MaterialTheme.typography.bodySmall)
                if (visible.isEmpty()) Text("Không tìm thấy lệnh phù hợp.")
                var shownGroups = 0
                LocalCommandExamples.groupOrder.forEach { group ->
                    val entries = visible.filter { it.group == group }
                    if (entries.isEmpty()) return@forEach
                    if (shownGroups > 0) androidx.compose.foundation.layout.Spacer(Modifier.height(8.dp))
                    shownGroups++
                    Text(group, style = MaterialTheme.typography.titleSmall)
                    entries.forEach { example ->
                        androidx.compose.material3.OutlinedCard(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(8.dp)) {
                                androidx.compose.material3.TextButton(onClick = { command = example.text; if (compactFull) showAllCommands = false },
                                    modifier = Modifier.fillMaxWidth().testTag("example_${example.id}")) {
                                    Text(example.text, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
                                }
                                if (example.note.isNotBlank()) Text(example.note, style = MaterialTheme.typography.bodySmall)
                                if (showAllCommands && example.aliases.isNotEmpty()) {
                                    androidx.compose.material3.TextButton(onClick = {
                                        expandedAliases = if (example.id in expandedAliases) expandedAliases - example.id else expandedAliases + example.id
                                    }, modifier = Modifier.testTag("expand_${example.id}")) {
                                        Text(if (example.id in expandedAliases) "Ẩn ${example.aliases.size} cách nói khác" else "Xem ${example.aliases.size} cách nói khác")
                                    }
                                    example.aliases.forEachIndexed { index, phrase ->
                                        if (example.id in expandedAliases || (commandSearch.isNotBlank() && LocalCommandExamples.matches(phrase, commandSearch))) {
                                            androidx.compose.material3.TextButton(onClick = { command = phrase; if (compactFull) showAllCommands = false },
                                                modifier = Modifier.fillMaxWidth().testTag("alias_${example.id}_${index + 1}")) {
                                                Text(phrase, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    Scaffold(modifier = modifier.fillMaxSize().testTag("dashboard")) { inset ->
        androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize().padding(inset).padding(8.dp)) {
            val compact = maxHeight < 400.dp
            val wide = maxWidth >= 700.dp || (compact && maxWidth >= 560.dp)
            val shortFullCatalog = showAllCommands && maxHeight < 520.dp
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                androidx.compose.foundation.layout.Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("DeX Assistant", style = MaterialTheme.typography.titleMedium)
                    androidx.compose.foundation.layout.Box {
                        androidx.compose.material3.TextButton(onClick = { advancedExpanded = true }, Modifier.testTag("advancedMenu")) { Text("Thêm ⋮") }
                        androidx.compose.material3.DropdownMenu(expanded = advancedExpanded, onDismissRequest = { advancedExpanded = false }) {
                            listOf("Cài đặt" to "settings", "Quota" to "quota", "Lịch sử" to "history", "Chẩn đoán" to "diagnostic").forEach { (label, key) ->
                                androidx.compose.material3.DropdownMenuItem(text = { Text(label) }, onClick = { dialog = key; advancedExpanded = false }, modifier = Modifier.testTag(key))
                            }
                        }
                    }
                }
                Text("${state.voiceState} · ${state.dexStatus} · ${state.accessibilityStatus}", maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth().clickable { detail = "${state.voiceDetail}\n${state.status}: ${state.message}\n${state.assistantReadiness}\n${state.dexStatus}\n${state.accessibilityStatus}\n${live.microphone}"; dialog = "detail" }.testTag("status"))
                Controls()
                if (chatActive) androidx.compose.material3.TextButton(onClick = onEndChat, Modifier.testTag("endChat")) { Text("Kết thúc trò chuyện") }
                if (wide) androidx.compose.foundation.layout.Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Transcript(Modifier.weight(1f).fillMaxSize())
                    CommandPane(Modifier.weight(1f).fillMaxSize(), shortFullCatalog)
                } else {
                    Transcript(Modifier.fillMaxWidth().height(if (compact || shortFullCatalog) 56.dp else 96.dp))
                    CommandPane(Modifier.fillMaxWidth().weight(1f), shortFullCatalog)
                }
            }
        }
    }
    if (dialog.isNotBlank()) androidx.compose.material3.AlertDialog(
        onDismissRequest = { dialog = ""; token = "" },
        confirmButton = { androidx.compose.material3.TextButton(onClick = { dialog = ""; token = "" }) { Text("Đóng") } },
        title = { Text(when (dialog) { "settings" -> "Cài đặt"; "quota" -> "AI quota"; "history" -> "Lịch sử"; "diagnostic" -> "Chẩn đoán"; else -> "Chi tiết" }) },
        text = {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when (dialog) {
                    "settings" -> {
                        AiProvidersSettings()
                        Text("Kết nối backend hiện tại (giữ để rollback)", style = MaterialTheme.typography.titleSmall)
                        OutlinedTextField(endpoint, { endpoint = it }, label = { Text("WebSocket endpoint") }, singleLine = true)
                        OutlinedTextField(token, { token = it }, label = { Text("Bearer token") }, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), singleLine = true)
                        Button(onClick = { onProvision(endpoint.trim(), token); token = "" }, enabled = endpoint.isNotBlank() && token.isNotBlank()) { Text("Provision and connect") }
                        Button(onClick = onConnect, enabled = state.endpoint.isNotBlank()) { Text("Connect") }
                        Button(onClick = onRefresh) { Text("Refresh health") }
                        Button(onClick = onApplyPermissions) { Text("Apply permission profile") }
                        Button(onClick = { context.startActivity(android.content.Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)) }) { Text("Trợ năng DeX") }
                        Text("Bật công tắc dịch vụ DeX Assistant để điều khiển số. Phím tắt Trợ năng có thể bật/tắt dịch vụ; chỉ giữ phím tắt nếu bạn muốn dùng nó. Ứng dụng hiển thị trạng thái thật và không tự bật dịch vụ.")
                        Button(onClick = { contactPermissions.launch(arrayOf(android.Manifest.permission.READ_CONTACTS, android.Manifest.permission.CALL_PHONE)) }) { Text("Quyền danh bạ và gọi điện") }
                        Button(onClick = { context.startActivity(android.content.Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }) { Text("Quyền truy cập phiên phát nhạc để tua") }
                        Text("Gọi điện luôn cần xác nhận bằng giọng nói. Nếu thiếu quyền gọi, ứng dụng chỉ mở bàn phím gọi. Quyền truy cập thông báo cho phép đọc trạng thái phiên phát nhạc để tua.")
                        OutlinedTextField(homeAddress, { if (it.length <= 500) homeAddress = it }, label = { Text("Địa chỉ nhà") })
                        OutlinedTextField(workAddress, { if (it.length <= 500) workAddress = it }, label = { Text("Địa chỉ công ty") })
                        Button(onClick = { localAliases.savePlace("home", homeAddress); localAliases.savePlace("work", workAddress); aliasFeedback = "Đã lưu địa chỉ trên thiết bị." }) { Text("Lưu địa chỉ") }
                        OutlinedTextField(contactAlias, { if (it.length <= 200) contactAlias = it }, label = { Text("Tên gọi, ví dụ mẹ") })
                        OutlinedTextField(contactName, { if (it.length <= 200) contactName = it }, label = { Text("Tên đầy đủ trong danh bạ; để trống để xóa") })
                        Button(onClick = { localAliases.saveContact(contactAlias, contactName); contactAlias = ""; contactName = ""; aliasFeedback = "Đã lưu tên gọi trên thiết bị." }, enabled = contactAlias.isNotBlank()) { Text("Lưu tên gọi") }
                        if (aliasFeedback.isNotBlank()) Text(aliasFeedback)
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
                    "history" -> { if (state.historyClearing) Text("Đang xóa lịch sử lệnh…"); state.historyError?.let { Text(it) }; state.history.forEach { Text("${it.route}: ${it.originalText} — ${it.outcome}") }; Button(onClick = onClearHistory, enabled = !state.historyClearing && (state.history.isNotEmpty() || state.historyError != null)) { Text("Clear command history") } }
                    "diagnostic" -> { Text(live.microphone); Button(onClick = onDiagnostic, Modifier.testTag("diagnosticListen")) { Text("Nghe thử · không thực hiện lệnh") }; Button(onClick = onRefresh) { Text("Làm mới trạng thái") } }
                    else -> Text(detail)
                }
            }
        },
    )
}
