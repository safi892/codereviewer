package com.secure.codereviewer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.secure.codereviewer.ui.components.AppBottomNavigation
import com.secure.codereviewer.ui.screens.codeinput.CodeInputScreen
import com.secure.codereviewer.ui.screens.dashboard.DashboardScreen
import com.secure.codereviewer.ui.screens.editor.EditorScreen
import com.secure.codereviewer.ui.screens.history.HistoryScreen
import com.secure.codereviewer.ui.screens.settings.SettingsScreen
import com.secure.codereviewer.ui.screens.welcome.WelcomeScreen
import kotlinx.coroutines.delay

enum class Screen {
    Splash,
    Welcome,
    Dashboard,
    History,
    CodeInput,
    Editor,
    Settings
}

@Composable
fun CodeReviewerApp() {
    var currentScreen by remember { mutableStateOf(Screen.Splash) }
    var lastOpenedFile by remember { mutableStateOf<LastOpenedFile?>(null) }

    LaunchedEffect(currentScreen) {
        if (currentScreen == Screen.Splash) {
            delay(1200)
            currentScreen = Screen.Welcome
        }
    }

    val showBottomBar = currentScreen in listOf(
        Screen.Dashboard, 
        Screen.History, 
        Screen.Editor, 
        Screen.Settings
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                AppBottomNavigation(
                    currentScreen = currentScreen,
                    onScreenChange = { currentScreen = it }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(
            bottom = if (showBottomBar) padding.calculateBottomPadding() else 0.dp
        )) {
            when (currentScreen) {
                Screen.Splash -> SplashScreen()
                Screen.Welcome -> WelcomeScreen(onStart = { currentScreen = Screen.Dashboard })
                Screen.Dashboard -> DashboardScreen(
                    onNewAnalysis = { currentScreen = Screen.CodeInput },
                    onOpenEditor = { currentScreen = Screen.Editor }
                )
                Screen.History -> HistoryScreen(
                    lastOpenedFile = lastOpenedFile,
                    onItemClick = { currentScreen = Screen.Editor }
                )
                Screen.CodeInput -> CodeInputScreen(
                    onBack = { currentScreen = Screen.Dashboard },
                    onAnalyze = { currentScreen = Screen.Editor }
                )
                Screen.Editor -> EditorScreen(
                    onBack = { currentScreen = Screen.Dashboard },
                    onFileOpened = { fileName ->
                        lastOpenedFile = LastOpenedFile(
                            name = fileName,
                            openedAtEpochMs = System.currentTimeMillis()
                        )
                    }
                )
                Screen.Settings -> SettingsScreen()
            }
        }
    }
}

@Composable
private fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    modifier = Modifier.size(80.dp),
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f),
                    shape = CircleShape
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Code,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "CppLens",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Intelligent Code Analysis",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                )
        }
    }
}
