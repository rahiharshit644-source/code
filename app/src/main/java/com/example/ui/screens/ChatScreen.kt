package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
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
import com.example.core.model.ChatMessage
import com.example.core.model.MessageSender
import com.example.ui.components.CodePilotTopBar
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.AppViewModel
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(viewModel: AppViewModel) {
    val activeProject by viewModel.activeProject.collectAsState()
    val chatMessages by viewModel.chatMessages.collectAsState()
    val isBusy by viewModel.isAgentBusy.collectAsState()
    val config by viewModel.omniRouteConfig.collectAsState()
    val pendingChangeSet by viewModel.pendingChangeSet.collectAsState()

    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val quickChips = listOf(
        "Fix Bug" to "Identify and fix the most critical bug in this project.",
        "Add Feature" to "Add a new modern feature to this project.",
        "Generate Tests" to "Generate comprehensive unit tests for core modules.",
        "Refactor Code" to "Refactor code to improve maintainability and clean architecture.",
        "Security Audit" to "Scan repository for security vulnerabilities and dangerous patterns.",
        "Explain Project" to "Provide an architectural overview and explain how this project works.",
        "Hinglish: Bug Fix" to "Is project me jo bugs hain unhe fix karke clean diff propose karo."
    )

    // Auto-scroll to bottom on new message
    LaunchedEffect(chatMessages.size, isBusy) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            CodePilotTopBar(
                title = "Coding Agent",
                subtitle = "${activeProject?.name ?: "No project"} • ${config.selectedModel}",
                canNavigateBack = true,
                onNavigateBack = { viewModel.navigateBack() },
                actions = {
                    IconButton(onClick = { viewModel.navigateTo(AppScreen.GATEWAY_CONFIG) }) {
                        Icon(Icons.Default.Tune, contentDescription = "Model Config", tint = PrimaryCyan)
                    }
                }
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceDark)
                    .imePadding()
            ) {
                // Pending Change Set notification bar
                if (pendingChangeSet != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AccentAmber.copy(alpha = 0.2f))
                            .clickable { viewModel.navigateTo(AppScreen.DIFF_REVIEW) }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Difference, contentDescription = null, tint = AccentAmber, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Review ${pendingChangeSet?.files?.size ?: 0} proposed file changes",
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 13.sp
                            )
                        }
                        Text("OPEN DIFF", color = AccentAmber, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                // Quick chips horizontal scroll
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for ((label, promptVal) in quickChips) {
                        AssistChip(
                            onClick = {
                                if (!isBusy) {
                                    viewModel.sendAgentPrompt(promptVal)
                                }
                            },
                            label = { Text(label, fontSize = 12.sp) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = SurfaceCard,
                                labelColor = PrimaryCyan
                            ),
                            border = AssistChipDefaults.assistChipBorder(borderColor = SurfaceVariantDark, enabled = true)
                        )
                    }
                }

                // Prompt Input Box
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("Ask CodePilot (English, Hindi, Hinglish)...", fontSize = 14.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        maxLines = 4,
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryCyan,
                            unfocusedBorderColor = SurfaceVariantDark,
                            focusedContainerColor = SurfaceVariantDark,
                            unfocusedContainerColor = SurfaceVariantDark
                        )
                    )

                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank() && !isBusy) {
                                val text = inputText.trim()
                                inputText = ""
                                viewModel.sendAgentPrompt(text)
                            }
                        },
                        enabled = inputText.isNotBlank() && !isBusy,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(if (inputText.isNotBlank() && !isBusy) PrimaryCyan else SurfaceVariantDark)
                    ) {
                        if (isBusy) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = TextPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = if (inputText.isNotBlank()) Color(0xFF032239) else TextMuted
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (chatMessages.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(PrimaryCyan.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Terminal, contentDescription = null, tint = PrimaryCyan, modifier = Modifier.size(32.dp))
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("CodePilot AI Agent Ready", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Ask questions, propose full refactors, generate multi-file edits, or audit security via OmniRoute.",
                        color = TextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    StatusBadge(
                        text = "Route: ${config.selectedModel}",
                        containerColor = SecondaryViolet.copy(alpha = 0.2f),
                        contentColor = SecondaryViolet,
                        icon = Icons.Default.CloudSync
                    )
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(chatMessages) { message ->
                    ChatMessageItem(message = message, onReviewDiff = {
                        viewModel.navigateTo(AppScreen.DIFF_REVIEW)
                    })
                }
            }
        }
    }
}

@Composable
fun ChatMessageItem(
    message: ChatMessage,
    onReviewDiff: () -> Unit
) {
    val isUser = message.sender == MessageSender.USER

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 4.dp)
        ) {
            if (!isUser) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(PrimaryCyan.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.SmartToy, contentDescription = null, tint = PrimaryCyan, modifier = Modifier.size(14.dp))
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text("CodePilot", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PrimaryCyan)
                if (message.modelBadge != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("(${message.modelBadge})", fontSize = 11.sp, color = TextMuted)
                }
            } else {
                Text("You", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
            }
        }

        Box(
            modifier = Modifier
                .widthIn(max = 340.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp
                    )
                )
                .background(if (isUser) PrimaryCyan.copy(alpha = 0.15f) else SurfaceCard)
                .border(
                    1.dp,
                    if (isUser) PrimaryCyan.copy(alpha = 0.4f) else SurfaceVariantDark,
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp
                    )
                )
                .padding(14.dp)
        ) {
            Column {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    lineHeight = 20.sp
                )

                // Associated change set review button
                if (message.associatedChangeSet != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onReviewDiff),
                        colors = CardDefaults.cardColors(containerColor = PrimaryCyan.copy(alpha = 0.12f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryCyan)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Rule, contentDescription = null, tint = PrimaryCyan, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Review Proposed Changes", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextPrimary)
                                Text("${message.associatedChangeSet.files.size} files modified", fontSize = 11.sp, color = TextSecondary)
                            }
                            Icon(Icons.Default.ArrowForward, contentDescription = null, tint = PrimaryCyan, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                if (message.isStreaming) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(12.dp),
                            strokeWidth = 1.5.dp,
                            color = PrimaryCyan
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Streaming response...", fontSize = 11.sp, color = PrimaryCyan)
                    }
                }
            }
        }
    }
}
