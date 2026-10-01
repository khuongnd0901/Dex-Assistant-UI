package com.khuongnd.dexassistant.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
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
                    controller::send, controller::retry)
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
    onSend: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var endpoint by rememberSaveable(state.endpoint) { mutableStateOf(state.endpoint) }
    var token by rememberSaveable { mutableStateOf("") }
    var command by rememberSaveable { mutableStateOf("") }

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("DeX Assistant", style = MaterialTheme.typography.headlineMedium)
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
            OutlinedTextField(command, { command = it }, Modifier.fillMaxWidth(),
                label = { Text("Command") }, singleLine = true)
            Button(
                onClick = { onSend(command); command = "" },
                enabled = state.status == "CONNECTED" || state.status == "COMPLETED",
            ) { Text("Send") }
            Button(onClick = onRetry, enabled = state.lastRequestId != null) { Text("Retry last request") }
            state.lastRequestId?.let { Text("Request: $it", style = MaterialTheme.typography.bodySmall) }
        }
    }
}
