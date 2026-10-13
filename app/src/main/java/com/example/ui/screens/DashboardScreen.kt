package com.example.ui.screens

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.CodePilotTopBar
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.AppViewModel

@Composable
fun DashboardScreen(viewModel: AppViewModel) {
    val activeProject by viewModel.activeProject.collectAsState()
    val projects by viewModel.projects.collectAsState()
    val config by viewModel.omniRouteConfig.collectAsState()
    val pendingChangeSet by viewModel.pendingChangeSet.collectAsState()
    val recentTasks by viewModel.taskDao.getAllRecentTasks().collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            CodePilotTopBar(
                title = "CodePilot AI",
                subtitle = "OmniRoute AI Gateway Edition",
                actions = {
                    IconButton(onClick = { viewModel.navigateTo(AppScreen.GATEWAY_CONFIG) }) {
                        Icon(Icons.Default.Tune, contentDescription = "Gateway Config", tint = PrimaryCyan)
                    }
                    IconButton(onClick = { viewModel.navigateTo(AppScreen.SETTINGS) }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = TextSecondary)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // Active Project Hero Banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ACTIVE WORKSPACE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryCyan,
                                letterSpacing = 1.sp
                            )
                            StatusBadge(
                                text = config.selectedModel,
                                containerColor = SecondaryViolet.copy(alpha = 0.2f),
                                contentColor = SecondaryViolet,
                                icon = Icons.Default.Bolt
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = activeProject?.name ?: "No Project Selected",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        Text(
                            text = activeProject?.description?.ifEmpty { "Ready for coding tasks and reviews." }
                                ?: "Create or import a repository to begin.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.navigateTo(AppScreen.CHAT) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyan)
                            ) {
                                Icon(Icons.Default.Chat, contentDescription = null, tint = Color(0xFF032239), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Ask Agent", color = Color(0xFF032239), fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = { viewModel.navigateTo(AppScreen.FILE_EXPLORER) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Files")
                            }
                        }
                    }
                }
            }

            // Pending Review Banner (if any change-set is waiting for user approval)
            if (pendingChangeSet != null) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.navigateTo(AppScreen.DIFF_REVIEW) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = AccentAmber.copy(alpha = 0.15f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AccentAmber)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Difference, contentDescription = null, tint = AccentAmber, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Pending Change Set Ready", fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text("${pendingChangeSet?.files?.size ?: 0} files proposed for review", fontSize = 13.sp, color = TextSecondary)
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = AccentAmber)
                        }
                    }
                }
            }

            // Quick Operations Grid
            item {
                Text("QUICK ACTIONS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DashboardActionTile(
                        title = "New Project",
                        icon = Icons.Default.AddCircle,
                        color = PrimaryCyan,
                        modifier = Modifier.weight(1f)
                    ) {
                        viewModel.navigateTo(AppScreen.NEW_PROJECT)
                    }
                    DashboardActionTile(
                        title = "Code Review",
                        icon = Icons.Default.FactCheck,
                        color = SecondaryViolet,
                        modifier = Modifier.weight(1f)
                    ) {
                        viewModel.runProjectReview()
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DashboardActionTile(
                        title = "Build & Test",
                        icon = Icons.Default.PlayArrow,
                        color = AccentGreen,
                        modifier = Modifier.weight(1f)
                    ) {
                        viewModel.runBuild()
                    }
                    DashboardActionTile(
                        title = "GitHub Sync",
                        icon = Icons.Default.CloudDownload,
                        color = Color(0xFF60A5FA),
                        modifier = Modifier.weight(1f)
                    ) {
                        viewModel.navigateTo(AppScreen.GITHUB)
                    }
                }
            }

            // Projects Library Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("PROJECTS (${projects.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 1.sp)
                    TextButton(onClick = { viewModel.navigateTo(AppScreen.PROJECTS_LIBRARY) }) {
                        Text("View All", color = PrimaryCyan, fontSize = 13.sp)
                    }
                }
            }

            items(projects.take(3)) { proj ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.selectProject(proj) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (proj.id == activeProject?.id) SurfaceVariantDark else SurfaceDark
                    ),
                    border = if (proj.id == activeProject?.id) androidx.compose.foundation.BorderStroke(1.5.dp, PrimaryCyan) else null
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(PrimaryCyan.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Folder, contentDescription = null, tint = PrimaryCyan)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(proj.name, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("${proj.primaryLanguage} • ${proj.fileCount} files", fontSize = 12.sp, color = TextSecondary)
                        }
                        if (proj.id == activeProject?.id) {
                            StatusBadge("ACTIVE", PrimaryCyan.copy(alpha = 0.2f), PrimaryCyan)
                        }
                    }
                }
            }

            // Recent Tasks Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("RECENT AGENT TASKS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 1.sp)
                    TextButton(onClick = { viewModel.navigateTo(AppScreen.TASK_HISTORY) }) {
                        Text("History", color = PrimaryCyan, fontSize = 13.sp)
                    }
                }
            }

            if (recentTasks.isEmpty()) {
                item {
                    Text("No agent tasks recorded yet. Click 'Ask Agent' to initiate your first coding task.", fontSize = 13.sp, color = TextMuted)
                }
            } else {
                items(recentTasks.take(3)) { task ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = task.prompt.take(35) + if (task.prompt.length > 35) "..." else "",
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                StatusBadge(
                                    text = task.status,
                                    containerColor = when (task.status) {
                                        "COMPLETED" -> AccentGreen.copy(alpha = 0.2f)
                                        "PROPOSED" -> AccentAmber.copy(alpha = 0.2f)
                                        else -> TextMuted.copy(alpha = 0.2f)
                                    },
                                    contentColor = when (task.status) {
                                        "COMPLETED" -> AccentGreen
                                        "PROPOSED" -> AccentAmber
                                        else -> TextSecondary
                                    }
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "Model: ${task.modelUsed}", fontSize = 11.sp, color = TextMuted)
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
fun DashboardActionTile(
    title: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(84.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            }
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TextPrimary)
        }
    }
}
