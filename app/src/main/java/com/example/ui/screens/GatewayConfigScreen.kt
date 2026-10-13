package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.OmniRouteConfig
import com.example.ui.components.CodePilotTopBar
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GatewayConfigScreen(viewModel: AppViewModel) {
    val config by viewModel.omniRouteConfig.collectAsState()
    val diagnostic by viewModel.connectionDiagnostic.collectAsState()
    val isTesting by viewModel.isTestingConnection.collectAsState()
    val models by viewModel.availableModels.collectAsState()
    val isLoadingModels by viewModel.isLoadingModels.collectAsState()

    var baseUrl by remember(config.baseUrl) { mutableStateOf(config.baseUrl) }
    var apiKey by remember(config.apiKey) { mutableStateOf(config.apiKey) }
    var isApiKeyVisible by remember { mutableStateOf(false) }

    var selectedModel by remember(config.selectedModel) { mutableStateOf(config.selectedModel) }
    var autoFallback by remember(config.autoFallback) { mutableStateOf(config.autoFallback) }
    var fallbackModel by remember(config.fallbackModel) { mutableStateOf(config.fallbackModel) }
    var timeoutSeconds by remember(config.timeoutSeconds) { mutableFloatStateOf(config.timeoutSeconds.toFloat()) }
    var contextBudget by remember(config.contextBudgetTokens) { mutableIntStateOf(config.contextBudgetTokens) }
    var streamResponse by remember(config.streamResponse) { mutableStateOf(config.streamResponse) }
    var temperature by remember(config.temperature) { mutableFloatStateOf(config.temperature) }

    var isModelDropdownExpanded by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            CodePilotTopBar(
                title = "OmniRoute AI Gateway",
                subtitle = "Routing & Model Configuration",
                canNavigateBack = true,
                onNavigateBack = { viewModel.navigateBack() },
                actions = {
                    TextButton(
                        onClick = {
                            viewModel.updateConfig(
                                OmniRouteConfig(
                                    baseUrl = baseUrl.trim(),
                                    apiKey = apiKey.trim(),
                                    selectedModel = selectedModel,
                                    autoFallback = autoFallback,
                                    fallbackModel = fallbackModel,
                                    timeoutSeconds = timeoutSeconds.toInt(),
                                    contextBudgetTokens = contextBudget,
                                    streamResponse = streamResponse,
                                    temperature = temperature
                                )
                            )
                        }
                    ) {
                        Text("Save", color = PrimaryCyan, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Explanatory Banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = PrimaryCyan.copy(alpha = 0.1f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryCyan.copy(alpha = 0.4f))
            ) {
                Row(modifier = Modifier.padding(14.dp)) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = PrimaryCyan, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("OmniRoute Gateway Architecture", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "OmniRoute runs locally or on your remote server (default port 20128). " +
                                    "For Android Emulator, use 'http://10.0.2.2:20128/v1' to reach your host PC. " +
                                    "For physical phones, use your PC's local WiFi IP (e.g. 'http://192.168.1.100:20128/v1') or your secure domain.",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // Connection Settings Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("GATEWAY CONNECTION", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PrimaryCyan, letterSpacing = 1.sp)

                    OutlinedTextField(
                        value = baseUrl,
                        onValueChange = { baseUrl = it },
                        label = { Text("Gateway Base URL") },
                        placeholder = { Text("http://10.0.2.2:20128/v1") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        label = { Text("Gateway API Key / Token (Optional)") },
                        placeholder = { Text("Leave blank if local unauthenticated") },
                        singleLine = true,
                        visualTransformation = if (isApiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isApiKeyVisible = !isApiKeyVisible }) {
                                Icon(
                                    imageVector = if (isApiKeyVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle Visibility"
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.updateConfig(
                                    config.copy(baseUrl = baseUrl.trim(), apiKey = apiKey.trim())
                                )
                                viewModel.testGatewayConnection()
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !isTesting
                        ) {
                            if (isTesting) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = TextPrimary, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Test Connection")
                            }
                        }

                        OutlinedButton(
                            onClick = { viewModel.discoverModels() },
                            modifier = Modifier.weight(1f),
                            enabled = !isLoadingModels
                        ) {
                            if (isLoadingModels) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Discover Models")
                            }
                        }
                    }

                    // Diagnostic result display
                    diagnostic?.let { diag ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (diag.success) AccentGreen.copy(alpha = 0.15f) else AccentRed.copy(alpha = 0.15f)
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (diag.success) AccentGreen else AccentRed
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = if (diag.success) "Diagnostic: Online" else "Diagnostic: Failed",
                                        fontWeight = FontWeight.Bold,
                                        color = if (diag.success) AccentGreen else AccentRed
                                    )
                                    Text("${diag.latencyMs} ms", fontSize = 12.sp, color = TextSecondary)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(diag.message, fontSize = 12.sp, color = TextPrimary)
                                if (diag.detectedRoute != null) {
                                    Text("Active Route: ${diag.detectedRoute}", fontSize = 11.sp, color = PrimaryCyan)
                                }
                                if (diag.rateLimitRemaining != null) {
                                    Text("Rate Limit Remaining: ${diag.rateLimitRemaining}", fontSize = 11.sp, color = AccentAmber)
                                }
                            }
                        }
                    }
                }
            }

            // Model Selection & Routing Preset
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("ROUTING & MODEL SELECTION", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SecondaryViolet, letterSpacing = 1.sp)

                    ExposedDropdownMenuBox(
                        expanded = isModelDropdownExpanded,
                        onExpandedChange = { isModelDropdownExpanded = !isModelDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedModel,
                            onValueChange = { selectedModel = it },
                            label = { Text("Active Coding Model / Route") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isModelDropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                        )

                        ExposedDropdownMenu(
                            expanded = isModelDropdownExpanded,
                            onDismissRequest = { isModelDropdownExpanded = false }
                        ) {
                            models.forEach { model ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(model.name, fontWeight = FontWeight.SemiBold)
                                                if (model.isFree) {
                                                    StatusBadge("FREE", AccentGreen.copy(alpha = 0.2f), AccentGreen)
                                                }
                                            }
                                            Text(model.description, fontSize = 11.sp, color = TextMuted)
                                        }
                                    },
                                    onClick = {
                                        selectedModel = model.id
                                        isModelDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Auto Fallback row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Automatic Fallback", fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            Text("Switch provider on rate-limit/quota failure", fontSize = 12.sp, color = TextSecondary)
                        }
                        Switch(checked = autoFallback, onCheckedChange = { autoFallback = it })
                    }

                    if (autoFallback) {
                        OutlinedTextField(
                            value = fallbackModel,
                            onValueChange = { fallbackModel = it },
                            label = { Text("Fallback Target Model") },
                            placeholder = { Text("auto") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Streaming Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Streaming Responses", fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            Text("Stream real-time tokens via SSE", fontSize = 12.sp, color = TextSecondary)
                        }
                        Switch(checked = streamResponse, onCheckedChange = { streamResponse = it })
                    }

                    // Timeout slider
                    Column {
                        Text("Request Timeout: ${timeoutSeconds.toInt()}s", fontSize = 13.sp, color = TextPrimary)
                        Slider(
                            value = timeoutSeconds,
                            onValueChange = { timeoutSeconds = it },
                            valueRange = 10f..180f,
                            steps = 16
                        )
                    }

                    // Context Budget
                    Column {
                        Text("Context Window Budget: $contextBudget tokens", fontSize = 13.sp, color = TextPrimary)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(8192, 16384, 32768, 65536).forEach { budget ->
                                FilterChip(
                                    selected = contextBudget == budget,
                                    onClick = { contextBudget = budget },
                                    label = { Text("${budget / 1024}k") }
                                )
                            }
                        }
                    }

                    // Temperature slider
                    Column {
                        Text("Sampling Temperature: ${"%.2f".format(temperature)}", fontSize = 13.sp, color = TextPrimary)
                        Slider(
                            value = temperature,
                            onValueChange = { temperature = it },
                            valueRange = 0.0f..1.0f
                        )
                    }
                }
            }

            // Credential Removal
            OutlinedButton(
                onClick = { viewModel.clearCredentials() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentRed)
            ) {
                Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Clear Credentials from Secure Store")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
