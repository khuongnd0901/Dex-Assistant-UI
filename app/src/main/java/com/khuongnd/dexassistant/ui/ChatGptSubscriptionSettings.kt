package com.khuongnd.dexassistant.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.khuongnd.dexassistant.ui.ai.ChatGptModelRepository
import com.khuongnd.dexassistant.ui.ai.ChatGptResponsesClient
import com.khuongnd.dexassistant.ui.ai.ResponsesStream
import com.khuongnd.dexassistant.ui.ai.SubscriptionAcceptance
import com.khuongnd.dexassistant.ui.ai.SubscriptionOperation
import com.khuongnd.dexassistant.ui.auth.ChatGptAuthManager
import com.khuongnd.dexassistant.ui.auth.ChatGptFailure
import com.khuongnd.dexassistant.ui.security.ChatGptSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/** No browser launch except the deliberate sign-in click. No raw auth/provider errors reach UI. */
@Composable
internal fun ChatGptSubscriptionSettings() {
    val context = LocalContext.current
    val auth = remember { ChatGptAuthManager.get(context) }
    val settings = remember { ChatGptSettings(context) }
    val scope = rememberCoroutineScope()
    var selected by remember { mutableStateOf(settings.selected()) }
    var connected by remember { mutableStateOf(auth.connected()) }
    var status by remember { mutableStateOf(if (connected) "CONNECTED" else auth.state.name) }
    var busy by remember { mutableStateOf(false) }
    var models by remember { mutableStateOf(emptyList<ChatGptModelRepository.Model>()) }
    var model by remember { mutableStateOf(settings.model()) }
    var menu by remember { mutableStateOf(false) }
    var preview by remember { mutableStateOf("") }
    val operation = remember { mutableStateOf<SubscriptionOperation?>(null) }
    DisposableEffect(auth) { onDispose { operation.value?.cancel(); if (auth.state == ChatGptAuthManager.State.AUTHENTICATING) auth.cancelLogin() } }
    fun open(uri: Uri) { context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    fun error(e: Throwable) {
        val safe = e as? ChatGptFailure ?: ChatGptFailure("ERROR")
        auth.failure(safe); status = auth.state.name; preview = ""; connected = auth.connected()
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("ChatGPT Subscription — $status", modifier = Modifier.testTag("subscriptionStatus"))
        Text(if (selected) "AI Chat và AI Command: ChatGPT Subscription" else "Kết nối và chọn ChatGPT để dùng AI Chat/AI Command.")
        Text("Dùng gói ChatGPT đã cấp quyền; không nhập OpenAI API key. Không tự chuyển sang API trả phí.")
        if (connected) {
            Text(auth.accountEmail().orEmpty())
            Text("Đã cấp quyền sử dụng gói ChatGPT. Các yêu cầu thử dùng hạn mức của tài khoản.")
        }
        Button(enabled = !busy, onClick = {
            AssistantController.shared(context).aiConnectionChanged()
            settings.clearSubscriptionVerification()
            busy = true; status = "AUTHENTICATING"; preview = ""
            scope.launch {
                try {
                    withContext(Dispatchers.IO) { auth.login { uri ->
                        // Browser API dispatch is posted to main; never wait for browser completion on main.
                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                            try { if (auth.state == ChatGptAuthManager.State.AUTHENTICATING) open(uri) } catch (_: Exception) { auth.cancelLogin() }
                        }
                    } }
                    connected = true; status = "CONNECTED"; models = emptyList()
                    settings.setEnabled(true)
                } catch (e: Exception) { error(e) }
                finally { busy = false }
            }
        }, modifier = Modifier.testTag("subscriptionSignIn")) { Text("Continue with ChatGPT") }
        if (busy) OutlinedButton(onClick = { operation.value?.cancel(); auth.cancelLogin() }) { Text("Hủy") }
        OutlinedButton(enabled = connected && !busy, onClick = {
            busy = true; status = "TOKEN_REFRESHING"
            scope.launch {
                try {
                    models = withContext(Dispatchers.IO) { ChatGptModelRepository(auth).load() }
                    if (models.none { it.slug == model }) {
                        model = ""; settings.setModel("")
                    }
                    status = if (models.isEmpty()) "Không có model được cấp quyền" else "CONNECTED"
                } catch (e: Exception) { models = emptyList(); error(e) }
                finally { busy = false }
            }
        }) { Text("Tải model được cấp quyền") }
        OutlinedButton(enabled = models.isNotEmpty() && !busy, onClick = { menu = true }) {
            Text("Model: " + (models.firstOrNull { it.slug == model }?.displayName ?: model.ifBlank { "Chưa chọn" }))
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            models.forEach { item -> DropdownMenuItem(text = { Text(item.displayName) }, onClick = {
                model = item.slug; settings.setModel(model); menu = false; preview = ""
            }) }
        }
        Button(enabled = connected && !busy && models.any { it.slug == model }, onClick = {
            busy = true; preview = ""; status = "Đang thử suy luận"; val op = SubscriptionOperation(); operation.value = op
            scope.launch {
                try {
                    val result = withContext(Dispatchers.IO) {
                        ChatGptResponsesClient(auth).response(ChatGptResponsesClient.payload(model,
                            JSONArray().put(JSONObject().put("role", "user").put("content", "Hãy chào tôi bằng một câu tiếng Việt.")),
                            "Reply briefly in Vietnamese."), op)
                    }
                    op.check(); preview = ResponsesStream.chatText(result)
                    settings.recordSubscriptionVerification(requireNotNull(auth.connectionKey()), model, false)
                    status = "CONNECTED — response.completed"
                } catch (e: Exception) { error(e) }
                finally { operation.value = null; busy = false }
            }
        }, modifier = Modifier.testTag("subscriptionTestInference")) { Text("Thử suy luận") }
        OutlinedButton(enabled = connected && !busy && models.any { it.slug == model }, onClick = {
            busy = true; preview = ""; status = "Đang thử function calling — không thực thi"
            val op = SubscriptionOperation(); operation.value = op
            scope.launch {
                try {
                    val count = withContext(Dispatchers.IO) {
                        val supported = setOf(com.khuongnd.dexassistant.ui.routing.ActionId.VOLUME_SET)
                        val registry = com.khuongnd.dexassistant.ui.routing.CommandRegistry.load(context)
                        val payload = ChatGptResponsesClient.payload(model,
                            JSONArray().put(JSONObject().put("role", "user").put("content", "Lập kế hoạch tắt âm lượng media.")),
                            "Call deviceCommand_VOLUME_SET with operation MUTE. This is a planning test only; no action executes.")
                            .put("tools", com.khuongnd.dexassistant.ui.ai.SubscriptionTools.schemas(supported, registry.appIds))
                        val response = ChatGptResponsesClient(auth).response(payload, op)
                        op.check()
                        val actions = com.khuongnd.dexassistant.ui.ai.SubscriptionTools.parse(response, registry, supported)
                        require(actions.size == 1 && actions.single().params["operation"] == "MUTE")
                        actions.size
                    }
                    settings.recordSubscriptionVerification(requireNotNull(auth.connectionKey()), model, true)
                    status = "CONNECTED — function calling hợp lệ"
                    preview = "$count action đã validate; không thực thi, không bật quyền điều khiển."
                } catch (e: Exception) { error(e) }
                finally { operation.value = null; busy = false }
            }
        }) { Text("Thử function calling (không thực thi)") }
        if (preview.isNotBlank()) Text(preview)
        Text("Sau khi cả hai phép thử thành công với tài khoản/model này, anh có thể chủ động chọn Subscription " +
            "để thử AI Chat và AI Command. Nghiệm thu voice/action còn mở; provider không tự đổi.")
        OutlinedButton(enabled = SubscriptionAcceptance.MANUAL_SELECTION_ACCEPTED && connected && !busy &&
            settings.subscriptionVerified(auth.connectionKey(), false) && settings.subscriptionVerified(auth.connectionKey(), true),
            onClick = { settings.select(); selected = true }) {
            Text(if (selected) "Đã chọn ChatGPT Subscription" else "Chọn Subscription để thử Chat/AI Command")
        }
        OutlinedButton(onClick = { open(Uri.parse("https://chatgpt.com/settings/usage")) }) { Text("Quản lý hạn mức ChatGPT") }
        OutlinedButton(enabled = !busy && connected, onClick = {
            AssistantController.shared(context).aiConnectionChanged()
            settings.clearSubscriptionVerification()
            busy = true; operation.value?.cancel(); preview = ""
            scope.launch {
                try {
                    val revoked = withContext(Dispatchers.IO) { auth.logout() }
                    connected = false; models = emptyList()
                    settings.setEnabled(false)
                    status = if (revoked) "NOT_CONNECTED" else "NOT_CONNECTED — chưa xác nhận thu hồi từ xa; ngắt app trong ChatGPT Settings"
                } catch (e: Exception) { error(e) }
                finally { busy = false }
            }
        }) { Text("Ngắt tài khoản") }
    }
}
