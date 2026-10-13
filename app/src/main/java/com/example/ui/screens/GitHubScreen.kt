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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.GitHubRepo
import com.example.ui.components.CodePilotTopBar
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.AppViewModel

@Composable
fun GitHubScreen(viewModel: AppViewModel) {
    val token by viewModel.githubToken.collectAsState()
    val repos by viewModel.githubRepos.collectAsState()
    val isLoading by viewModel.isLoadingGithub.collectAsState()

    var tokenInput by remember(token) { mutableStateOf(token) }

    LaunchedEffect(token) {
        if (token.isNotBlank() && repos.isEmpty()) {
            viewModel.fetchGitHubRepos(token)
        }
    }

    Scaffold(
        topBar = {
            CodePilotTopBar(
                title = "GitHub Integration",
                subtitle = if (token.isNotBlank()) "Connected" else "Personal Access Token required",
                canNavigateBack = true,
                onNavigateBack = { viewModel.navigateBack() }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Token setup Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("GITHUB CREDENTIALS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PrimaryCyan, letterSpacing = 1.sp)

                    OutlinedTextField(
                        value = tokenInput,
                        onValueChange = { tokenInput = it },
                        label = { Text("Personal Access Token (PAT)") },
                        placeholder = { Text("ghp_...") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.connectGitHub(tokenInput.trim()) },
                            modifier = Modifier.weight(1f),
                            enabled = tokenInput.isNotBlank() && !isLoading
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Connect & Fetch")
                            }
                        }

                        if (token.isNotBlank()) {
                            OutlinedButton(
                                onClick = {
                                    tokenInput = ""
                                    viewModel.connectGitHub("")
                                }
                            ) {
                                Text("Disconnect")
                            }
                        }
                    }
                }
            }

            // Repositories listing
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("AUTHORIZED REPOSITORIES (${repos.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 1.sp)
                if (token.isNotBlank()) {
                    IconButton(onClick = { viewModel.fetchGitHubRepos(token) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = PrimaryCyan)
                    }
                }
            }

            if (repos.isEmpty() && !isLoading) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (token.isBlank()) "Enter a GitHub token with 'repo' scope to browse your repositories."
                        else "No repositories found for this account.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(repos) { repo ->
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
                                    Text(repo.fullName, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)
                                    StatusBadge(
                                        text = if (repo.isPrivate) "PRIVATE" else "PUBLIC",
                                        containerColor = if (repo.isPrivate) AccentAmber.copy(alpha = 0.2f) else AccentGreen.copy(alpha = 0.2f),
                                        contentColor = if (repo.isPrivate) AccentAmber else AccentGreen
                                    )
                                }

                                if (!repo.description.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(repo.description, fontSize = 12.sp, color = TextSecondary, maxLines = 2)
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Branch: ${repo.defaultBranch}", fontSize = 11.sp, color = TextMuted)
                                    Button(
                                        onClick = { viewModel.importGitHubRepo(repo) },
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyan)
                                    ) {
                                        Icon(Icons.Default.Download, contentDescription = null, tint = Color(0xFF032239), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Import to Sandbox", color = Color(0xFF032239), fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
