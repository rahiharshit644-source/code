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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.ProjectEntity
import com.example.ui.components.CodePilotTopBar
import com.example.ui.components.ConfirmationDialog
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.AppViewModel

@Composable
fun ProjectsLibraryScreen(viewModel: AppViewModel) {
    val projects by viewModel.projects.collectAsState()
    val activeProject by viewModel.activeProject.collectAsState()

    var projectToDelete by remember { mutableStateOf<ProjectEntity?>(null) }

    Scaffold(
        topBar = {
            CodePilotTopBar(
                title = "Projects Library",
                subtitle = "${projects.size} available workspaces",
                canNavigateBack = true,
                onNavigateBack = { viewModel.navigateBack() }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.navigateTo(AppScreen.NEW_PROJECT) },
                containerColor = PrimaryCyan
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Project", tint = Color(0xFF032239))
            }
        }
    ) { padding ->
        if (projects.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null, tint = TextMuted, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No projects yet", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Text("Create or import a repository to begin", color = TextSecondary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.navigateTo(AppScreen.NEW_PROJECT) }) {
                        Text("Create Project")
                    }
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
                items(projects) { proj ->
                    val isActive = proj.id == activeProject?.id

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.selectProject(proj) },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = if (isActive) SurfaceVariantDark else SurfaceDark),
                        border = if (isActive) androidx.compose.foundation.BorderStroke(1.5.dp, PrimaryCyan) else null
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = proj.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    if (isActive) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        StatusBadge("ACTIVE", PrimaryCyan.copy(alpha = 0.2f), PrimaryCyan)
                                    }
                                }
                                StatusBadge(
                                    text = proj.primaryLanguage,
                                    containerColor = SecondaryViolet.copy(alpha = 0.2f),
                                    contentColor = SecondaryViolet
                                )
                            }

                            if (proj.description.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(proj.description, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "${proj.fileCount} source files • Branch: ${proj.currentBranch}",
                                fontSize = 12.sp,
                                color = TextMuted
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                IconButton(onClick = {
                                    viewModel.selectProject(proj)
                                    viewModel.navigateTo(AppScreen.FILE_EXPLORER)
                                }) {
                                    Icon(Icons.Default.Folder, contentDescription = "Files", tint = PrimaryCyan)
                                }
                                IconButton(onClick = {
                                    viewModel.selectProject(proj)
                                    viewModel.shareProjectZip()
                                }) {
                                    Icon(Icons.Default.Share, contentDescription = "Share ZIP", tint = AccentGreen)
                                }
                                IconButton(onClick = {
                                    projectToDelete = proj
                                }) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = AccentRed)
                                }
                            }
                        }
                    }
                }
            }
        }

        projectToDelete?.let { p ->
            ConfirmationDialog(
                title = "Delete Project?",
                message = "Are you sure you want to permanently delete '${p.name}' and all its source files and task history?",
                confirmText = "Delete",
                isDestructive = true,
                onConfirm = {
                    viewModel.deleteProject(p)
                    projectToDelete = null
                },
                onDismiss = { projectToDelete = null }
            )
        }
    }
}
