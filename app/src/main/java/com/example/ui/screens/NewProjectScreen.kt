package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.CodePilotTopBar
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.AppViewModel

@Composable
fun NewProjectScreen(viewModel: AppViewModel) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var projectName by remember { mutableStateOf("") }
    var projectDescription by remember { mutableStateOf("") }
    var selectedTemplate by remember { mutableStateOf("compose") }

    val templates = listOf(
        Triple("compose", "Android Jetpack Compose", "Modern UI with Compose, ViewModel & Material 3"),
        Triple("python", "Python CLI / Service", "Fast Python script with pytest structure"),
        Triple("react", "React + TypeScript", "Web application with Vite & TypeScript"),
        Triple("kotlin", "Kotlin Multiplatform / Core", "Standard Kotlin library structure")
    )

    // ActivityResult launcher for picking ZIP archive from SAF
    val zipPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    val fallbackName = uri.lastPathSegment?.substringAfterLast("/")?.removeSuffix(".zip") ?: "ImportedProject"
                    viewModel.importZipArchive(inputStream, fallbackName)
                }
            } catch (e: Exception) {
                viewModel.showStatus("Error reading file: ${e.message}")
            }
        }
    }

    Scaffold(
        topBar = {
            CodePilotTopBar(
                title = "New or Import Project",
                subtitle = "Initialize or unpack repository",
                canNavigateBack = true,
                onNavigateBack = { viewModel.navigateBack() }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Import section card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "IMPORT EXISTING REPOSITORY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryCyan,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Import from ZIP Archive (Safe Extract)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Protected against Zip Slip traversal and bomb attacks. Extracts safely into sandbox.",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { zipPickerLauncher.launch("application/zip") },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyan)
                        ) {
                            Icon(Icons.Default.UploadFile, contentDescription = null, tint = Color(0xFF032239), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Select ZIP", color = Color(0xFF032239), fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { viewModel.navigateTo(AppScreen.GITHUB) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("From GitHub")
                        }
                    }
                }
            }

            HorizontalDivider(color = SurfaceVariantDark)

            // Create from Template section
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "CREATE NEW FROM TEMPLATE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = SecondaryViolet,
                    letterSpacing = 1.sp
                )

                OutlinedTextField(
                    value = projectName,
                    onValueChange = { projectName = it },
                    label = { Text("Project Name") },
                    placeholder = { Text("e.g. MyAwesomeApp") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = projectDescription,
                    onValueChange = { projectDescription = it },
                    label = { Text("Description (Optional)") },
                    placeholder = { Text("Brief purpose of the project") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Select Starter Template:", fontWeight = FontWeight.SemiBold, color = TextPrimary)

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for ((key, title, desc) in templates) {
                        val isSelected = selectedTemplate == key
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedTemplate = key },
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) SurfaceVariantDark else SurfaceDark
                            ),
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, PrimaryCyan) else null
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedTemplate = key }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(title, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    Text(desc, fontSize = 12.sp, color = TextSecondary)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        val name = projectName.trim().ifEmpty { "Project-${System.currentTimeMillis() % 1000}" }
                        viewModel.createProject(name, projectDescription.trim(), selectedTemplate)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyan)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF032239))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Initialize Workspace", color = Color(0xFF032239), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}
