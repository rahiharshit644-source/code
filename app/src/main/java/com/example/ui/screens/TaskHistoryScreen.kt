package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.CodePilotTopBar
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TaskHistoryScreen(viewModel: AppViewModel) {
    val tasks by viewModel.taskDao.getAllRecentTasks().collectAsState(initial = emptyList())
    val activeProject by viewModel.activeProject.collectAsState()

    val dateFormatter = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }

    Scaffold(
        topBar = {
            CodePilotTopBar(
                title = "Task History & Audit",
                subtitle = "${tasks.size} recorded agent sessions",
                canNavigateBack = true,
                onNavigateBack = { viewModel.navigateBack() }
            )
        }
    ) { padding ->
        if (tasks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.History, contentDescription = null, tint = TextMuted, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No Task History", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Text("Task executions and rollback checkpoints will appear here.", color = TextSecondary)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(tasks) { task ->
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
                                    text = dateFormatter.format(Date(task.timestamp)),
                                    fontSize = 11.sp,
                                    color = TextMuted,
                                    fontWeight = FontWeight.SemiBold
                                )
                                StatusBadge(
                                    text = task.status,
                                    containerColor = when (task.status) {
                                        "COMPLETED" -> AccentGreen.copy(alpha = 0.2f)
                                        "PROPOSED" -> AccentAmber.copy(alpha = 0.2f)
                                        else -> PrimaryCyan.copy(alpha = 0.2f)
                                    },
                                    contentColor = when (task.status) {
                                        "COMPLETED" -> AccentGreen
                                        "PROPOSED" -> AccentAmber
                                        else -> PrimaryCyan
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = task.prompt,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 14.sp
                            )

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Model: ${task.modelUsed} • ${task.summary}",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )

                            if (task.rollbackBackupId != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    OutlinedButton(
                                        onClick = { viewModel.rollbackTask(task) }
                                    ) {
                                        Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Rollback")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
