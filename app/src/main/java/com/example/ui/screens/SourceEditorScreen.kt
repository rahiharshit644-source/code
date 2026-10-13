package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import com.example.ui.components.CodeEditorView
import com.example.ui.components.CodePilotTopBar
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.AppViewModel

@Composable
fun SourceEditorScreen(viewModel: AppViewModel) {
    val selectedFile by viewModel.selectedFile.collectAsState()
    val editorContent by viewModel.editorContent.collectAsState()
    val isDirty by viewModel.isEditorDirty.collectAsState()

    val fileExt = selectedFile?.name?.substringAfterLast(".", "kt") ?: "kt"

    Scaffold(
        topBar = {
            CodePilotTopBar(
                title = (selectedFile?.name ?: "Editor") + if (isDirty) " *" else "",
                subtitle = selectedFile?.relativePath,
                canNavigateBack = true,
                onNavigateBack = { viewModel.navigateBack() },
                actions = {
                    // Ask Agent button
                    IconButton(onClick = {
                        val path = selectedFile?.relativePath ?: ""
                        viewModel.navigateTo(AppScreen.CHAT)
                        viewModel.sendAgentPrompt("Explain and optimize the logic in `$path`.")
                    }) {
                        Icon(Icons.Default.SmartToy, contentDescription = "Ask Agent", tint = SecondaryViolet)
                    }

                    // Save file button
                    IconButton(
                        onClick = { viewModel.saveEditorContent() },
                        enabled = isDirty
                    ) {
                        Icon(
                            Icons.Default.Save,
                            contentDescription = "Save",
                            tint = if (isDirty) PrimaryCyan else TextMuted
                        )
                    }
                }
            )
        },
        bottomBar = {
            // Footer status bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceDark)
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${editorContent.lines().size} lines • ${editorContent.length} chars",
                    fontSize = 11.sp,
                    color = TextMuted
                )
                Text(
                    text = "UTF-8 • $fileExt",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PrimaryCyan
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            CodeEditorView(
                codeText = editorContent,
                onCodeChange = { viewModel.updateEditorContent(it) },
                fileExtension = fileExt
            )
        }
    }
}
