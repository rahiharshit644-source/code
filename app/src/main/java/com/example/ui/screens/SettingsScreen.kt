package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.CodePilotTopBar
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppViewModel
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(viewModel: AppViewModel) {
    val remoteWorkerUrl by viewModel.remoteWorkerUrl.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var workerInput by remember(remoteWorkerUrl) { mutableStateOf(remoteWorkerUrl) }
    var selectedLang by remember { mutableStateOf("en") }

    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            CodePilotTopBar(
                title = "Application Settings",
                subtitle = "Security & Workspace Preferences",
                canNavigateBack = true,
                onNavigateBack = { viewModel.navigateBack() }
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
            // Security Posture Audit Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("SECURITY & HARDENING", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AccentGreen, letterSpacing = 1.sp)
                        StatusBadge("VERIFIED", AccentGreen.copy(alpha = 0.2f), AccentGreen)
                    }

                    SecurityAuditItem(title = "Zip Slip Traversal Defense", status = "Active", desc = "Archive entries verified against canonical project root.")
                    SecurityAuditItem(title = "Decompression Bomb Protection", status = "Active", desc = "Entry count and size cap strictly enforced (<150MB).")
                    SecurityAuditItem(title = "Sandbox Isolation", status = "Active", desc = "All project writes isolated to app internal files directory.")
                    SecurityAuditItem(title = "Token Redaction in Logs", status = "Active", desc = "API keys and PATs redacted from terminal telemetry.")
                }
            }

            // Remote Build Worker
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("REMOTE BUILD WORKER", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PrimaryCyan, letterSpacing = 1.sp)
                    Text("Optionally connect a remote Gradle/Docker builder for complete cloud compilation of APKs.", fontSize = 12.sp, color = TextSecondary)

                    OutlinedTextField(
                        value = workerInput,
                        onValueChange = { workerInput = it },
                        label = { Text("Worker Base URL (Optional)") },
                        placeholder = { Text("e.g. http://my-worker.internal:8080") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                viewModel.preferencesManager.updateRemoteWorkerUrl(workerInput.trim())
                                viewModel.showStatus("Build worker URL updated.")
                            }
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Save Worker URL")
                    }
                }
            }

            // Language & Instruction Support
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("LANGUAGE SUPPORT", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SecondaryViolet, letterSpacing = 1.sp)
                    Text("CodePilot AI natively understands instructions in English, Hindi, and Hinglish.", fontSize = 12.sp, color = TextSecondary)

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = selectedLang == "en",
                            onClick = { selectedLang = "en" },
                            label = { Text("English") }
                        )
                        FilterChip(
                            selected = selectedLang == "hi",
                            onClick = { selectedLang = "hi" },
                            label = { Text("Hindi (हिंदी)") }
                        )
                        FilterChip(
                            selected = selectedLang == "hinglish",
                            onClick = { selectedLang = "hinglish" },
                            label = { Text("Hinglish") }
                        )
                    }
                }
            }

            // About CodePilot Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("ABOUT CODEPILOT AI", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 1.sp)
                    Text("CodePilot AI — OmniRoute Edition", fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("Version 1.0.0 (Production Native Build)", fontSize = 12.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Designed for real software development workflows on Android. " +
                                "Integrates with OmniRoute AI gateway for resilient model routing, fallback recovery, and multi-provider token efficiency.",
                        fontSize = 12.sp,
                        color = TextMuted,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun SecurityAuditItem(title: String, status: String, desc: String) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TextPrimary)
            Text(status, fontSize = 11.sp, color = AccentGreen, fontWeight = FontWeight.Bold)
        }
        Text(desc, fontSize = 11.sp, color = TextMuted)
    }
}
