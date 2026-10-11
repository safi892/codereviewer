package com.secure.codereviewer.ui.screens.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.secure.codereviewer.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNewAnalysis: () -> Unit,
    onOpenEditor: () -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "C++ Code Reviewer",
                        style = MaterialTheme.typography.headlineSmall, 
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            WelcomeCard()
            
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                SectionTitle("Overview")
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    StatCard(
                        title = "Analyses",
                        value = "1,284",
                        icon = Icons.Default.Code,
                        modifier = Modifier.weight(1f)
                    )

                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                SectionTitle("Actions")
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    ActionCard(
                        title = "Analyze",
                        icon = Icons.Default.Add,
                        onClick = onNewAnalysis,
                        modifier = Modifier.weight(1f),
                        gradientColors = listOf(Color(0xFF4A6CF7), Color(0xFF6C89FF))
                    )
                    ActionCard(
                        title = "Editor",
                        icon = Icons.Default.Edit,
                        onClick = onOpenEditor,
                        modifier = Modifier.weight(1f),
                        gradientColors = listOf(Color(0xFF1F2933), Color(0xFF52606D))
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                SectionTitle("Recent Session")
                RecentActivityCard(onClick = onOpenEditor)
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
