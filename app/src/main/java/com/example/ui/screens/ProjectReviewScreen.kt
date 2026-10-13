package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.FindingSeverity
import com.example.core.model.ReviewFinding
import com.example.ui.components.CodePilotTopBar
import com.example.ui.components.SeverityBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.AppViewModel

@Composable
fun ProjectReviewScreen(viewModel: AppViewModel) {
    val report by viewModel.reviewReport.collectAsState()
    val isReviewing by viewModel.isReviewing.collectAsState()
    val context = LocalContext.current

    var selectedFilter by remember { mutableStateOf<FindingSeverity?>(null) }

    Scaffold(
        topBar = {
            CodePilotTopBar(
                title = "Code Review Report",
                subtitle = report?.projectName ?: "Static Analysis",
                canNavigateBack = true,
                onNavigateBack = { viewModel.navigateBack() },
                actions = {
                    IconButton(onClick = { viewModel.runProjectReview() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Rerun", tint = PrimaryCyan)
                    }
                    if (report != null) {
                        IconButton(onClick = {
                            val md = viewModel.reviewEngine.exportReportAsMarkdown(report!!)
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, md)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Review Report"))
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Export Report", tint = AccentGreen)
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (isReviewing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = PrimaryCyan)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Scanning project repository...", fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("Checking security, exception handling, manifests & architecture", color = TextSecondary)
                }
            }
        } else if (report == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.FactCheck, contentDescription = null, tint = TextMuted, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No Review Run Yet", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.runProjectReview() }) {
                        Text("Run Static Analysis")
                    }
                }
            }
        } else {
            val currentReport = report!!
            val filteredFindings = remember(currentReport, selectedFilter) {
                if (selectedFilter == null) currentReport.findings
                else currentReport.findings.filter { it.severity == selectedFilter }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Health Score Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceCard)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            currentReport.healthScore >= 80 -> AccentGreen.copy(alpha = 0.2f)
                                            currentReport.healthScore >= 60 -> AccentAmber.copy(alpha = 0.2f)
                                            else -> AccentRed.copy(alpha = 0.2f)
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${currentReport.healthScore}",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        currentReport.healthScore >= 80 -> AccentGreen
                                        currentReport.healthScore >= 60 -> AccentAmber
                                        else -> AccentRed
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text("CODE HEALTH SCORE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 1.sp)
                                Text(
                                    text = currentReport.summary,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }

                // Filter chips
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedFilter == null,
                            onClick = { selectedFilter = null },
                            label = { Text("All (${currentReport.findings.size})") }
                        )
                        FilterChip(
                            selected = selectedFilter == FindingSeverity.CRITICAL,
                            onClick = { selectedFilter = if (selectedFilter == FindingSeverity.CRITICAL) null else FindingSeverity.CRITICAL },
                            label = { Text("Critical") }
                        )
                        FilterChip(
                            selected = selectedFilter == FindingSeverity.HIGH,
                            onClick = { selectedFilter = if (selectedFilter == FindingSeverity.HIGH) null else FindingSeverity.HIGH },
                            label = { Text("High") }
                        )
                    }
                }

                // Findings list
                items(filteredFindings) { finding ->
                    FindingCard(
                        finding = finding,
                        onAutoFix = {
                            viewModel.navigateTo(AppScreen.CHAT)
                            viewModel.sendAgentPrompt(
                                "Fix the code review finding '${finding.title}' in `${finding.affectedFile}`. Suggested fix: ${finding.suggestedFix}"
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun FindingCard(
    finding: ReviewFinding,
    onAutoFix: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = finding.title,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                SeverityBadge(severity = finding.severity)
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "${finding.affectedFile} ${if (finding.lineNumber != null) "(Line ${finding.lineNumber})" else ""}",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = PrimaryCyan
            )

            Spacer(modifier = Modifier.height(6.dp))
            Text(text = finding.explanation, fontSize = 13.sp, color = TextSecondary)

            if (finding.codeSnippet.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(EditorBackground)
                        .padding(8.dp)
                ) {
                    Text(
                        text = finding.codeSnippet,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = Color(0xFFE2E8F0)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Fix: ${finding.suggestedFix}",
                    fontSize = 11.sp,
                    color = AccentGreen,
                    modifier = Modifier.weight(1f)
                )

                TextButton(onClick = onAutoFix) {
                    Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = PrimaryCyan, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Auto-Fix", color = PrimaryCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
