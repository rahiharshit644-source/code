package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.ChangeType
import com.example.core.model.FileDiffItem
import com.example.ui.components.CodePilotTopBar
import com.example.ui.components.RiskLevelBadge
import com.example.ui.components.StatusBadge
import com.example.ui.components.UnifiedDiffViewer
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.AppViewModel

@Composable
fun DiffReviewScreen(viewModel: AppViewModel) {
    val pendingChangeSet by viewModel.pendingChangeSet.collectAsState()
    var selectedFileIndex by remember { mutableIntStateOf(0) }

    val files = pendingChangeSet?.files ?: emptyList()
    val currentDiffItem: FileDiffItem? = files.getOrNull(selectedFileIndex)

    Scaffold(
        topBar = {
            CodePilotTopBar(
                title = "Change Set Review",
                subtitle = "${files.count { it.isApproved }}/${files.size} approved",
                canNavigateBack = true,
                onNavigateBack = { viewModel.navigateBack() }
            )
        },
        bottomBar = {
            Surface(
                color = SurfaceDark,
                tonalElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.rejectPendingChangeSet() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentRed)
                    ) {
                        Text("Reject All")
                    }

                    Button(
                        onClick = { viewModel.applyPendingChangeSet() },
                        modifier = Modifier.weight(1.5f),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyan),
                        enabled = files.any { it.isApproved }
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF032239), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Apply Changes", color = Color(0xFF032239), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { padding ->
        if (pendingChangeSet == null || files.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(56.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No Pending Changes", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Text("All agent proposals have been reviewed or applied.", color = TextSecondary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.navigateTo(AppScreen.CHAT) }) {
                        Text("Return to Chat")
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // File selector tabs
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceDark)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(files.indices.toList()) { index ->
                        val item = files[index]
                        val isSelected = selectedFileIndex == index
                        Card(
                            modifier = Modifier.clickable { selectedFileIndex = index },
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) PrimaryCyan.copy(alpha = 0.2f) else SurfaceCard
                            ),
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, PrimaryCyan) else null
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = item.isApproved,
                                    onCheckedChange = { viewModel.toggleFileApproval(item.id) },
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = item.filePath.substringAfterLast("/"),
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) PrimaryCyan else TextPrimary
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = SurfaceVariantDark)

                // Selected File Details and Unified Diff Viewer
                currentDiffItem?.let { item ->
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
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
                                        Text(
                                            text = item.filePath,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = TextPrimary
                                        )
                                        RiskLevelBadge(riskLevel = item.riskLevel)
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        StatusBadge(
                                            text = item.changeType.name,
                                            containerColor = when (item.changeType) {
                                                ChangeType.CREATE -> AccentGreen.copy(alpha = 0.2f)
                                                ChangeType.DELETE -> AccentRed.copy(alpha = 0.2f)
                                                else -> PrimaryCyan.copy(alpha = 0.2f)
                                            },
                                            contentColor = when (item.changeType) {
                                                ChangeType.CREATE -> AccentGreen
                                                ChangeType.DELETE -> AccentRed
                                                else -> PrimaryCyan
                                            }
                                        )
                                        if (item.reason.isNotBlank()) {
                                            Text(
                                                text = item.reason,
                                                fontSize = 12.sp,
                                                color = TextSecondary,
                                                modifier = Modifier.align(Alignment.CenterVertically)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Text(
                                text = "UNIFIED DIFF",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted,
                                letterSpacing = 1.sp
                            )
                        }

                        item {
                            UnifiedDiffViewer(diffText = item.diffUnified)
                        }
                    }
                }
            }
        }
    }
}
