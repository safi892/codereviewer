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
import com.secure.codereviewer.data.api.AuthManager
import com.secure.codereviewer.ui.components.AppBottomNavigation
import com.secure.codereviewer.ui.screens.codeinput.CodeInputScreen
import com.secure.codereviewer.ui.screens.dashboard.DashboardScreen
import com.secure.codereviewer.ui.screens.editor.EditorScreen
import com.secure.codereviewer.ui.screens.history.HistoryScreen
import com.secure.codereviewer.ui.screens.history.HistoryUiItem
import com.secure.codereviewer.ui.screens.privacy.PrivacyPolicyScreen
import com.secure.codereviewer.ui.screens.settings.SettingsScreen
import com.secure.codereviewer.ui.screens.welcome.LoginScreen
import com.secure.codereviewer.ui.screens.welcome.SignupScreen
import com.secure.codereviewer.ui.screens.welcome.WelcomeScreen
import kotlinx.coroutines.delay

enum class Screen {
    Splash,
    Welcome,
    Login,
    Signup,
    Dashboard,
    History,
    CodeInput,
    Editor,
    Settings,
    PrivacyPolicy
}

@Composable
fun CodeReviewerApp() {
    var currentScreen by remember { mutableStateOf(Screen.Splash) }
    var pendingEditorContent by remember { mutableStateOf<String?>(null) }
    var pendingEditorFileName by remember { mutableStateOf<String?>(null) }
    var pendingEditorContentKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(currentScreen) {
        if (currentScreen == Screen.Splash) {
            delay(1200)
            // Check if user is already logged in
            currentScreen = if (AuthManager.isLoggedIn()) Screen.Dashboard else Screen.Welcome
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
                Screen.Welcome -> WelcomeScreen(
                    onStart = { currentScreen = Screen.Login },
                    onLoginClick = { currentScreen = Screen.Login }
                )
                Screen.Login -> LoginScreen(
                    onLoginSuccess = { currentScreen = Screen.Dashboard },
                    onSignupClick = { currentScreen = Screen.Signup }
                )
                Screen.Signup -> SignupScreen(
                    onSignupSuccess = { currentScreen = Screen.Dashboard },
                    onLoginClick = { currentScreen = Screen.Login }
                )
                Screen.Dashboard -> DashboardScreen(
                    onNewAnalysis = { currentScreen = Screen.CodeInput },
                    onOpenEditor = { currentScreen = Screen.Editor }
                )
                Screen.History -> HistoryScreen(
                    onItemClick = { historyItem ->
                        pendingEditorContent = buildHistoryContent(historyItem)
                        pendingEditorFileName = "analysis-${historyItem.id}.cpp"
                        pendingEditorContentKey += 1
                        currentScreen = Screen.Editor
                    }
                )
                Screen.CodeInput -> CodeInputScreen(
                    onBack = { currentScreen = Screen.Dashboard },
                    onAnalyze = { code, fileName ->
                        pendingEditorContent = code
                        pendingEditorFileName = fileName ?: "pasted-code.cpp"
                        pendingEditorContentKey += 1
                        currentScreen = Screen.Editor
                    },
                    onOpenEditor = { code, fileName ->
                        pendingEditorContent = code
                        pendingEditorFileName = fileName
                        pendingEditorContentKey += 1
                        currentScreen = Screen.Editor
                    }
                )
                Screen.Editor -> EditorScreen(
                    onBack = { currentScreen = Screen.Dashboard },
                    onFileSaved = { _ -> },
                    initialContent = pendingEditorContent,
                    initialFileName = pendingEditorFileName,
                    initialContentKey = pendingEditorContentKey,
                    onInitialContentConsumed = {
                        pendingEditorContent = null
                        pendingEditorFileName = null
                    }
                )
                Screen.Settings -> SettingsScreen(
                    onLogout = { 
                        AuthManager.logout()
                        currentScreen = Screen.Welcome 
                    },
                    onOpenPrivacyPolicy = { currentScreen = Screen.PrivacyPolicy }
                )
                Screen.PrivacyPolicy -> PrivacyPolicyScreen(
                    onBack = { currentScreen = Screen.Settings }
                )
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

private fun buildHistoryContent(item: HistoryUiItem): String {
    val code = item.commentedCode.trim()
    val explanation = item.explanation.trim()
    
    val codeLines = code.lines()
    val firstFunctionIndex = codeLines.indexOfFirst { line ->
        val trimmed = line.trim()
        trimmed.isNotEmpty() &&
            "(" in trimmed &&
            !trimmed.startsWith("//") &&
            !trimmed.startsWith("/*") &&
            !trimmed.startsWith("*")
    }
    
    val header = if (firstFunctionIndex > 0) {
        codeLines.take(firstFunctionIndex).joinToString("\n").trimEnd()
    } else {
        ""
    }
    
    val body = if (firstFunctionIndex >= 0) {
        codeLines.drop(firstFunctionIndex).joinToString("\n")
    } else {
        code
    }
    
    val explanationBlock = if (explanation.isNotBlank()) {
        explanation.lines().filter { it.isNotBlank() }.joinToString("\n") { "// $it" }
    } else {
        ""
    }
    
    return listOf(header, explanationBlock, body)
        .filter { it.isNotBlank() }
        .joinToString("\n\n")
}
