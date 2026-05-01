package com.secure.codereviewer.ui.screens.privacy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Privacy Policy",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
//            Text(
//                text = "Effective date: May 1, 2026",
//                style = MaterialTheme.typography.bodySmall,
//                color = MaterialTheme.colorScheme.onSurfaceVariant
//            )

            PolicySection(
                title = "Overview",
                body = "CppLens analyzes C++ code you open or paste to provide explanations, diagnostics, and improvement suggestions. This policy explains how the app handles data during that process."
            )

            PolicySection(
                title = "Data we process",
                body = "- Account info such as name and email if you sign in.\n" +
                    "- Code content that you send for analysis.\n" +
                    "- File names and timestamps for recent activity shown in the app.\n" +
                    "- Basic app events needed for stability (crash or error logs)."
            )

            PolicySection(
                title = "How analysis works",
                body = "When you run analysis, the app sends your code to the configured analysis server to generate results. If you use a local server, the data stays on your network. If you use a remote server, that server processes the code to return the response."
            )

            PolicySection(
                title = "Local storage",
                body = "Files are saved where you choose on your device or storage provider. Recent history shown in the app is stored locally while the app is running."
            )

            PolicySection(
                title = "Authentication",
                body = "If you log in, the app stores authentication data on your device to keep you signed in. You can log out at any time from Settings."
            )

            PolicySection(
                title = "Questions",
                body = "If you have questions about privacy or security, contact your app administrator or the team that provided your CppLens build."
            )

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun PolicySection(title: String, body: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
