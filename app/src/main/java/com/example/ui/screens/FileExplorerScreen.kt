package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.ProjectFile
import com.example.ui.components.CodePilotTopBar
import com.example.ui.components.ConfirmationDialog
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.AppViewModel

@Composable
fun FileExplorerScreen(viewModel: AppViewModel) {
    val activeProject by viewModel.activeProject.collectAsState()
    val currentPath by viewModel.currentExplorerPath.collectAsState()
    val files by viewModel.projectFiles.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }
    var newFileName by remember { mutableStateOf("") }
    var isNewItemFolder by remember { mutableStateOf(false) }
    var fileToDelete by remember { mutableStateOf<ProjectFile?>(null) }

    LaunchedEffect(activeProject?.id, currentPath) {
        viewModel.refreshProjectFiles()
    }

    val displayedFiles = remember(files, searchQuery) {
        if (searchQuery.isBlank()) files
        else files.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            CodePilotTopBar(
                title = "File Explorer",
                subtitle = activeProject?.name ?: "No project",
                canNavigateBack = true,
                onNavigateBack = {
                    if (currentPath.isNotEmpty()) {
                        val parent = currentPath.substringBeforeLast("/", "")
                        viewModel.navigateExplorerTo(parent)
                    } else {
                        viewModel.navigateBack()
                    }
                },
                actions = {
                    IconButton(onClick = { showCreateDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add File/Folder", tint = PrimaryCyan)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search files in project...", fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextMuted)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Breadcrumb navigation
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "root",
                    color = if (currentPath.isEmpty()) PrimaryCyan else TextSecondary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.clickable { viewModel.navigateExplorerTo("") }
                )
                if (currentPath.isNotEmpty()) {
                    val segments = currentPath.split("/")
                    var accum = ""
                    for (seg in segments) {
                        Text(" / ", color = TextMuted, fontSize = 13.sp)
                        accum = if (accum.isEmpty()) seg else "$accum/$seg"
                        val target = accum
                        Text(
                            text = seg,
                            color = PrimaryCyan,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable { viewModel.navigateExplorerTo(target) }
                        )
                    }
                }
            }

            HorizontalDivider(color = SurfaceVariantDark, modifier = Modifier.padding(vertical = 4.dp))

            // File / Folder listing
            if (displayedFiles.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isNotEmpty()) "No files match '$searchQuery'" else "Empty directory",
                        color = TextMuted
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp)
                ) {
                    items(displayedFiles) { file ->
                        ListItem(
                            headlineContent = {
                                Text(file.name, fontWeight = if (file.isDirectory) FontWeight.Bold else FontWeight.Normal, color = TextPrimary)
                            },
                            supportingContent = {
                                if (!file.isDirectory) {
                                    Text(
                                        "${file.sizeBytes / 1024} KB • ${file.relativePath}",
                                        fontSize = 11.sp,
                                        color = TextMuted
                                    )
                                }
                            },
                            leadingContent = {
                                Icon(
                                    imageVector = if (file.isDirectory) Icons.Default.Folder else getIconForFile(file.name),
                                    contentDescription = null,
                                    tint = if (file.isDirectory) PrimaryCyan else SecondaryViolet
                                )
                            },
                            trailingContent = {
                                Row {
                                    IconButton(onClick = { fileToDelete = file }) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = TextMuted, modifier = Modifier.size(20.dp))
                                    }
                                }
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.clickable {
                                viewModel.openFileInEditor(file)
                            }
                        )
                        HorizontalDivider(color = SurfaceVariantDark.copy(alpha = 0.5f), thickness = 0.5.dp)
                    }
                }
            }
        }

        // Create Dialog
        if (showCreateDialog) {
            AlertDialog(
                onDismissRequest = { showCreateDialog = false },
                title = { Text("Create New Entry") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = newFileName,
                            onValueChange = { newFileName = it },
                            label = { Text("File / Folder Name") },
                            placeholder = { Text("e.g. MyService.kt") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = isNewItemFolder,
                                onCheckedChange = { isNewItemFolder = it }
                            )
                            Text("Create as Folder")
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        if (newFileName.isNotBlank()) {
                            viewModel.createNewFileOrFolder(newFileName.trim(), isNewItemFolder)
                            newFileName = ""
                            isNewItemFolder = false
                            showCreateDialog = false
                        }
                    }) {
                        Text("Create")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showCreateDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Delete Dialog
        fileToDelete?.let { f ->
            ConfirmationDialog(
                title = "Delete ${if (f.isDirectory) "Folder" else "File"}?",
                message = "Are you sure you want to permanently delete '${f.name}'?",
                confirmText = "Delete",
                isDestructive = true,
                onConfirm = {
                    viewModel.deleteFileFromExplorer(f)
                    fileToDelete = null
                },
                onDismiss = { fileToDelete = null }
            )
        }
    }
}

fun getIconForFile(fileName: String) = when {
    fileName.endsWith(".kt") || fileName.endsWith(".java") -> Icons.Default.Code
    fileName.endsWith(".json") || fileName.endsWith(".xml") || fileName.endsWith(".yaml") || fileName.endsWith(".yml") -> Icons.Default.DataObject
    fileName.endsWith(".md") || fileName.endsWith(".txt") -> Icons.Default.Description
    fileName.endsWith(".gradle") || fileName.endsWith(".kts") -> Icons.Default.Build
    else -> Icons.Default.InsertDriveFile
}
