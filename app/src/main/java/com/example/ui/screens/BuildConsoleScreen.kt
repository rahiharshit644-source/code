package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.CodePilotTopBar
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.AppViewModel

@Composable
fun BuildConsoleScreen(viewModel: AppViewModel) {
    val buildResult by viewModel.buildResult.collectAsState()
    val isBuilding by viewModel.isBuilding.collectAsState()
    val remoteWorkerUrl by viewModel.remoteWorkerUrl.collectAsState()

    var selectedProfile by remember(remoteWorkerUrl) {
        mutableStateOf(if (remoteWorkerUrl.isBlank()) "Local Static Checks" else "Debug Build")
    }
    val profiles = if (remoteWorkerUrl.isBlank()) {
        listOf("Local Static Checks")
    } else {
        listOf("Debug Build", "Unit Tests", "Release Bundle")
    }

    val verticalScroll = rememberScrollState()
    val horizontalScroll = rememberScrollState()

    Scaffold(
        topBar = {
            CodePilotTopBar(
                title = "Build & Test Console",
                subtitle = if (remoteWorkerUrl.isNotBlank()) "Worker: $remoteWorkerUrl" else "Local Device Engine",
                canNavigateBack = true,
                onNavigateBack = { viewModel.navigateBack() },
                actions = {
                    IconButton(onClick = { viewModel.runBuild(selectedProfile) }) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Run Build", tint = AccentGreen)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Profile selector chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (p in profiles) {
                    FilterChip(
                        selected = selectedProfile == p,
                        onClick = { selectedProfile = p },
                        label = { Text(p, fontSize = 12.sp) }
                    )
                }
            }

            // Build Action & Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("BUILD EXECUTION STATUS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 1.sp)
                            Text(selectedProfile, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }

                        if (isBuilding) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = PrimaryCyan, strokeWidth = 2.dp)
                        } else if (buildResult != null) {
                            val localChecks = buildResult!!.logs.contains("CodePilot Local Static Checker")
                            StatusBadge(
                                text = if (localChecks) {
                                    if (buildResult!!.success) "STATIC CHECKS PASSED" else "STATIC CHECKS FAILED"
                                } else {
                                    if (buildResult!!.success) "PASSED (Exit 0)" else "FAILED (Exit ${buildResult!!.exitCode})"
                                },
                                containerColor = if (buildResult!!.success) AccentGreen.copy(alpha = 0.2f) else AccentRed.copy(alpha = 0.2f),
                                contentColor = if (buildResult!!.success) AccentGreen else AccentRed
                            )
                        }
                    }

                    if (buildResult != null && !isBuilding) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Duration: ${buildResult!!.durationMs}ms", fontSize = 12.sp, color = TextSecondary)
                            val localChecks = buildResult!!.logs.contains("CodePilot Local Static Checker")
                            Text(
                                if (localChecks) "Files: ${buildResult!!.passedTests} passed • ${buildResult!!.failedTests} issues"
                                else "Tests: ${buildResult!!.passedTests} passed • ${buildResult!!.failedTests} failed",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }

                        if (!buildResult!!.success) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    viewModel.navigateTo(AppScreen.CHAT)
                                    viewModel.sendAgentPrompt(
                                        "The build failed with the following error output. Diagnose the cause and propose fixes:\n\n${buildResult!!.logs.take(500)}"
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Explain Build Error with AI")
                            }
                        }
                    }
                }
            }

            // Terminal Logs Area
            Text("EXECUTION LOGS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 1.sp)

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(EditorBackground)
                    .padding(12.dp)
            ) {
                Text(
                    text = buildResult?.logs?.ifEmpty { "Ready. Tap 'Play' icon above to trigger build execution." }
                        ?: "No build executed yet for current project.",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = Color(0xFFE2E8F0),
                    modifier = Modifier
                        .verticalScroll(verticalScroll)
                        .horizontalScroll(horizontalScroll)
                )
            }
        }
    }
}
