package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.PrimaryCyan
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.AppViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                MainAppScaffold(viewModel = viewModel)
            }
        }
    }
}

data class BottomNavItem(
    val screen: AppScreen,
    val title: String,
    val icon: ImageVector
)

@Composable
fun MainAppScaffold(viewModel: AppViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val pendingChangeSet by viewModel.pendingChangeSet.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Handle back button presses gracefully
    BackHandler(enabled = currentScreen != AppScreen.DASHBOARD) {
        viewModel.navigateBack()
    }

    LaunchedEffect(statusMessage) {
        statusMessage?.let { msg ->
            coroutineScope.launch {
                snackbarHostState.showSnackbar(msg)
                viewModel.clearStatus()
            }
        }
    }

    val navItems = listOf(
        BottomNavItem(AppScreen.DASHBOARD, "Dashboard", Icons.Default.Dashboard),
        BottomNavItem(AppScreen.CHAT, "Agent", Icons.Default.SmartToy),
        BottomNavItem(AppScreen.FILE_EXPLORER, "Files", Icons.Default.Folder),
        BottomNavItem(AppScreen.DIFF_REVIEW, "Diffs", Icons.Default.Difference),
        BottomNavItem(AppScreen.GATEWAY_CONFIG, "Gateway", Icons.Default.Tune)
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            // Show bottom navigation bar
            NavigationBar(
                containerColor = SurfaceDark,
                windowInsets = WindowInsets.navigationBars
            ) {
                for (item in navItems) {
                    val isSelected = currentScreen == item.screen
                    val hasBadge = item.screen == AppScreen.DIFF_REVIEW && pendingChangeSet != null

                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { viewModel.navigateTo(item.screen) },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (hasBadge) {
                                        Badge { Text("${pendingChangeSet?.files?.size ?: 0}") }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.title,
                                    tint = if (isSelected) PrimaryCyan else TextMuted
                                )
                            }
                        },
                        label = {
                            Text(
                                text = item.title,
                                color = if (isSelected) PrimaryCyan else TextMuted
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = PrimaryCyan.copy(alpha = 0.15f)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                AppScreen.DASHBOARD -> DashboardScreen(viewModel = viewModel)
                AppScreen.PROJECTS_LIBRARY -> ProjectsLibraryScreen(viewModel = viewModel)
                AppScreen.NEW_PROJECT -> NewProjectScreen(viewModel = viewModel)
                AppScreen.CHAT -> ChatScreen(viewModel = viewModel)
                AppScreen.FILE_EXPLORER -> FileExplorerScreen(viewModel = viewModel)
                AppScreen.SOURCE_EDITOR -> SourceEditorScreen(viewModel = viewModel)
                AppScreen.DIFF_REVIEW -> DiffReviewScreen(viewModel = viewModel)
                AppScreen.PROJECT_REVIEW -> ProjectReviewScreen(viewModel = viewModel)
                AppScreen.BUILD_CONSOLE -> BuildConsoleScreen(viewModel = viewModel)
                AppScreen.TASK_HISTORY -> TaskHistoryScreen(viewModel = viewModel)
                AppScreen.GATEWAY_CONFIG -> GatewayConfigScreen(viewModel = viewModel)
                AppScreen.GITHUB -> GitHubScreen(viewModel = viewModel)
                AppScreen.EXPORT -> ExportScreen(viewModel = viewModel)
                AppScreen.SETTINGS -> SettingsScreen(viewModel = viewModel)
            }
        }
    }
}
