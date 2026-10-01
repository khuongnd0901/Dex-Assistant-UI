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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.khuongnd.dexassistant.ui.theme.DexAssistantTheme

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
                    controller::startVoice, controller::stopVoice)
            }
        }
    }

    override fun onDestroy() {
        controller.stop()
        super.onDestroy()
    }
}

@androidx.compose.runtime.Composable
fun AssistantScreen(
    state: UiState,
    onProvision: (String, String) -> Unit,
    onConnect: () -> Unit,
    onRefresh: () -> Unit,
    onApplyPermissions: () -> Unit,
    onStartVoice: () -> Unit,
    onStopVoice: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var endpoint by rememberSaveable(state.endpoint) { mutableStateOf(state.endpoint) }
    var token by rememberSaveable { mutableStateOf("") }

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("DeX Assistant", style = MaterialTheme.typography.headlineMedium)
            Text("Administration", style = MaterialTheme.typography.titleMedium)
            Text("${state.status}: ${state.message}")
            OutlinedTextField(endpoint, { endpoint = it }, Modifier.fillMaxWidth(),
                label = { Text("WebSocket endpoint") },
                placeholder = { Text("wss://host/ws/v1/assistant") }, singleLine = true)
            OutlinedTextField(token, { token = it }, Modifier.fillMaxWidth(),
                label = { Text("Bearer token") }, visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), singleLine = true)
            Button(
                onClick = { onProvision(endpoint.trim(), token); token = "" },
                enabled = endpoint.isNotBlank() && token.isNotBlank(),
            ) { Text("Provision and connect") }
            Button(onClick = onConnect, enabled = state.endpoint.isNotBlank()) { Text("Connect") }
            Text("Health", style = MaterialTheme.typography.titleMedium)
            Text("Device Owner: ${if (state.deviceOwner) "ready" else "not verified"}")
            Text("Voice: ${state.voiceState} — ${state.voiceDetail}")
            Text("Default assistant readiness: ${state.assistantReadiness}")
            Text("Backend: ${state.status}")
            Button(onClick = onRefresh) { Text("Refresh health") }
            Button(onClick = onApplyPermissions) { Text("Apply permission profile") }
            Button(onClick = onStartVoice) { Text("Start voice service") }
            Button(onClick = onStopVoice) { Text("Emergency stop microphone") }
            if (state.capabilities.isNotEmpty()) {
                val granted = state.capabilities.values.count { it == "GRANTED" }
                val action = state.capabilities.values.count { it == "USER_ACTION_REQUIRED" }
                Text("Permissions: $granted granted, $action require Settings")
            }
        }
    }
}
