package com.khuongnd.dexassistant.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.khuongnd.dexassistant.ui.ai.AiKeyProbe
import com.khuongnd.dexassistant.ui.ai.ProbeStatus
import com.khuongnd.dexassistant.ui.security.AiCredentialStore
import com.khuongnd.dexassistant.ui.security.AiProvider
import com.khuongnd.dexassistant.ui.security.AiProviderSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Per-provider configuration; direct Command supports only implemented, free-confirmed adapters. */
@Composable
fun AiProvidersSettings() {
    val context = LocalContext.current.applicationContext
    val providerSettings = remember(context) { AiProviderSettings(context) }
    var selected by remember { mutableStateOf(AiProvider.GEMINI) }
    var providerMenu by remember { mutableStateOf(false) }
    var preferred by remember { mutableStateOf(providerSettings.defaultProvider()) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("AI Providers", style = MaterialTheme.typography.titleMedium)
        Text("Local Command chạy trên máy; AI Command và Chat gọi API trực tiếp, không dùng backend.",
            style = MaterialTheme.typography.bodySmall)
        Text("Provider ưu tiên: " + preferred.displayName, style = MaterialTheme.typography.bodySmall)
        OutlinedButton(onClick = { providerMenu = true }, modifier = Modifier.testTag("aiProviderPicker")) {
            Text("Chọn provider: " + selected.displayName)
        }
        DropdownMenu(expanded = providerMenu, onDismissRequest = { providerMenu = false }) {
            AiProvider.entries.forEach { provider ->
                DropdownMenuItem(
                    text = { Text(provider.displayName + if (provider == preferred) " (ưu tiên)" else "") },
                    onClick = { selected = provider; providerMenu = false },
                    modifier = Modifier.testTag("provider_" + provider.name.lowercase()),
                )
            }
        }
        key(selected) { AiProviderEditor(selected) { preferred = selected } }
    }
}

@Composable
private fun AiProviderEditor(provider: AiProvider, onPreferred: () -> Unit) {
    val context = LocalContext.current.applicationContext
    val secrets = remember(context) { AiCredentialStore(context) }
    val settings = remember(context) { AiProviderSettings(context) }
    val probe = remember { AiKeyProbe() }
    val scope = rememberCoroutineScope()
    var input by remember { mutableStateOf("") }
    var configured by remember { mutableStateOf(secrets.hasKey(provider)) }
    var enabled by remember { mutableStateOf(settings.enabled(provider)) }
    var model by remember { mutableStateOf(settings.model(provider)) }
    var confirmedFree by remember { mutableStateOf(settings.freeTierConfirmed(provider)) }
    var preferred by remember { mutableStateOf(settings.defaultProvider() == provider) }
    var models by remember { mutableStateOf(emptyList<String>()) }
    var modelsMenu by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var revision by remember { mutableIntStateOf(0) }
    var status by remember {
        mutableStateOf(if (configured) "Đã lưu key" else "Chưa cấu hình API key")
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(provider.displayName, style = MaterialTheme.typography.titleSmall)
            Switch(
                checked = enabled,
                enabled = !busy,
                onCheckedChange = { next ->
                    runCatching { settings.setEnabled(provider, next) }
                        .onSuccess { enabled = next }
                        .onFailure { status = "Không thể cập nhật provider" }
                },
                modifier = Modifier.testTag("aiProviderEnabled"),
            )
        }
        Text(if (configured) "API key: Đã cấu hình" else "API key: Chưa cấu hình")
        Text(if (provider.supportsDirectCommands) "Direct Command adapter: khả dụng (thử nghiệm)"
            else "Chỉ lưu cấu hình: adapter Direct Command chưa kích hoạt",
            style = MaterialTheme.typography.bodySmall)
        OutlinedButton(onClick = {
            runCatching { settings.setDefaultProvider(provider) }
                .onSuccess { preferred = true; onPreferred(); status = "Đã đặt provider ưu tiên" }
                .onFailure { status = "Không thể lưu provider ưu tiên" }
        }, enabled = !preferred && !busy, modifier = Modifier.testTag("setDefaultAiProvider")) {
            Text(if (preferred) "Provider ưu tiên hiện tại" else "Đặt làm provider ưu tiên")
        }
        OutlinedTextField(
            value = input,
            onValueChange = { if (it.length <= 1024) input = it },
            label = { Text("API key mới") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth().testTag("aiProviderApiKey"),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Button(
                enabled = !busy && input.isNotBlank(),
                onClick = {
                    val newValue = input
                    val nextRevision = ++revision
                    busy = true
                    scope.launch {
                        val result = withContext(Dispatchers.IO) {
                            runCatching { secrets.save(provider, newValue) }
                        }
                        if (revision == nextRevision) {
                            busy = false
                            if (result.isSuccess) {
                                input = ""
                                configured = true
                                status = "Đã lưu key vào thiết bị"
                            } else status = "Lưu thất bại: kiểm tra định dạng key"
                        }
                    }
                },
                modifier = Modifier.testTag("saveAiKey"),
            ) { Text("Lưu / cập nhật") }
            OutlinedButton(
                enabled = !busy && configured,
                onClick = {
                    val nextRevision = ++revision
                    busy = true
                    scope.launch {
                        val result = withContext(Dispatchers.IO) {
                            runCatching {
                                secrets.delete(provider)
                                settings.setEnabled(provider, false)
                            }
                        }
                        if (revision == nextRevision) {
                            busy = false
                            if (result.isSuccess) {
                                input = ""
                                configured = false
                                enabled = false
                                status = "Đã xóa API key"
                            } else status = "Không thể xóa API key"
                        }
                    }
                },
                modifier = Modifier.testTag("deleteAiKey"),
            ) { Text("Xóa key") }
        }
        OutlinedTextField(
            value = model,
            onValueChange = { if (it.length <= 128) model = it },
            label = { Text("Model mặc định của provider (Model ID)") },
            supportingText = { Text("Chỉ nhập model đã xác minh miễn phí trong tài khoản của bạn.") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("aiProviderModel"),
        )
        OutlinedButton(
            enabled = !busy && configured,
            onClick = {
                busy = true
                status = "Đang tải danh sách model..."
                scope.launch {
                    val result = withContext(Dispatchers.IO) {
                        secrets.load(provider)?.let { probe.listModels(provider, it) }
                    }
                    busy = false
                    if (result?.status == ProbeStatus.AUTHENTICATED) {
                        models = result.models
                        modelsMenu = models.isNotEmpty()
                        status = if (models.isEmpty()) "Không có model trong tài khoản" else
                            "Chọn model từ danh sách; chưa xác nhận Free Tier"
                    } else status = "Không tải được model: " + (result?.status?.name ?: "KEY_MISSING")
                }
            }, modifier = Modifier.testTag("loadAiModels")
        ) { Text("Tải model từ API") }
        DropdownMenu(expanded = modelsMenu, onDismissRequest = { modelsMenu = false }) {
            models.forEach { id ->
                DropdownMenuItem(text = { Text(id) }, onClick = { model = id; modelsMenu = false })
            }
        }
        OutlinedButton(
            enabled = !busy,
            onClick = {
                runCatching { settings.setModel(provider, model) }
                    .onSuccess { confirmedFree = false; status = "Đã lưu model ID; cần xác nhận Free Tier" }
                    .onFailure { status = "Model ID không hợp lệ" }
            },
        ) { Text("Lưu model") }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Checkbox(checked = confirmedFree, enabled = !busy && model.isNotBlank(),
                onCheckedChange = { next ->
                    runCatching { settings.setFreeTierConfirmed(provider, next) }
                        .onSuccess { confirmedFree = next }
                        .onFailure { status = "Không thể lưu xác nhận Free Tier" }
                }, modifier = Modifier.testTag("confirmFreeTier"))
            Text("Tôi đã xác minh model này thuộc Free Tier của tài khoản. Test key không chứng minh miễn phí.",
                style = MaterialTheme.typography.bodySmall)
        }
        OutlinedButton(
            enabled = !busy && configured,
            onClick = {
                val nextRevision = ++revision
                busy = true
                status = "Đang kiểm tra quyền truy cập..."
                scope.launch {
                    val result = withContext(Dispatchers.IO) {
                        secrets.load(provider)?.let { probe.test(provider, it) }
                    }
                    if (revision == nextRevision) {
                        busy = false
                        status = when (result?.status) {
                            ProbeStatus.AUTHENTICATED -> "Key hợp lệ; chưa xác nhận model Free Tier"
                            ProbeStatus.INVALID_KEY -> "API key không hợp lệ (401)"
                            ProbeStatus.ACCESS_DENIED -> "Truy cập bị từ chối (403)"
                            ProbeStatus.QUOTA_LIMITED -> "Đã chạm giới hạn API (429)"
                            ProbeStatus.NETWORK_ERROR -> "Lỗi mạng hoặc timeout"
                            ProbeStatus.HTTP_ERROR -> "Không thể kiểm tra: HTTP " + result.httpStatus
                            null -> "Key không còn trên thiết bị"
                        }
                    }
                }
            },
            modifier = Modifier.testTag("testAiKey"),
        ) { Text("Kiểm tra API key (không chạy AI)") }
        Text(status, modifier = Modifier.testTag("aiProviderStatus"))
    }
}
