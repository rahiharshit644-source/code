package com.example.ui.screens

import androidx.compose.foundation.layout.*
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
import com.example.ui.components.CodePilotTopBar
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppViewModel

@Composable
fun ExportScreen(viewModel: AppViewModel) {
    val activeProject by viewModel.activeProject.collectAsState()

    Scaffold(
        topBar = {
            CodePilotTopBar(
                title = "Export & Share",
                subtitle = activeProject?.name ?: "No project selected",
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
            // Full ZIP Export Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Archive, contentDescription = null, tint = PrimaryCyan, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Full Project ZIP Archive", fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("Packs all directories, manifests, and source files into a standard .zip", fontSize = 12.sp, color = TextSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { viewModel.shareProjectZip() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyan),
                        enabled = activeProject != null
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = Color(0xFF032239), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Share via Android Share Sheet", color = Color(0xFF032239), fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Export Single Artifact Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.FilePresent, contentDescription = null, tint = SecondaryViolet, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Export Review Report", fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("Export static code health analysis in Markdown format", fontSize = 12.sp, color = TextSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = { viewModel.runProjectReview() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.FactCheck, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Generate & Share Report")
                    }
                }
            }
        }
    }
}
