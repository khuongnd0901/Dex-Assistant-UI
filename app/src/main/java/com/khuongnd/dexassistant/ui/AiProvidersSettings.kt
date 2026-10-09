package com.khuongnd.dexassistant.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
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

/** Only provisions cloud credentials. Direct AI Command and Chat routing are not enabled here. */
@Composable
fun AiProvidersSettings() {
    val context = LocalContext.current.applicationContext
    val providerSettings = remember(context) { AiProviderSettings(context) }
    var directMode by remember { mutableStateOf(providerSettings.directMode()) }
    var transportStatus by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf(AiProvider.GEMINI) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("AI Providers", style = MaterialTheme.typography.titleMedium)
        Text("Key chỉ lưu trên điện thoại. Direct API chỉ áp dụng cho Command; Chat vẫn dùng backend.",
            style = MaterialTheme.typography.bodySmall)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Direct AI Command (thử nghiệm)")
            Switch(checked = directMode, onCheckedChange = { next ->
                runCatching { providerSettings.setDirectMode(next) }
                    .onSuccess { directMode = next; transportStatus = if (next) "Command phức tạp gọi AI trực tiếp." else "Command phức tạp dùng backend." }
                    .onFailure { transportStatus = "Không thể lưu chế độ AI" }
            }, modifier = Modifier.testTag("directAiCommand"))
        }
        if (transportStatus.isNotBlank()) Text(transportStatus, style = MaterialTheme.typography.bodySmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AiProvider.entries.forEach { provider ->
                OutlinedButton(onClick = { selected = provider },
                    modifier = Modifier.testTag("provider_" + provider.name.lowercase())) {
                    Text(if (provider == selected) provider.displayName + " ✓" else provider.displayName)
                }
            }
        }
        key(selected) { AiProviderEditor(selected) }
    }
}

@Composable
private fun AiProviderEditor(provider: AiProvider) {
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
            label = { Text("Model ID (Free Tier)") },
            supportingText = { Text("Chỉ nhập model đã xác minh miễn phí trong tài khoản của bạn.") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("aiProviderModel"),
        )
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
