package com.secure.codereviewer.ui.screens.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.secure.codereviewer.ui.components.AnalysisHistoryItem
import com.secure.codereviewer.ui.screens.RecentSavedFile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    recentFiles: List<RecentSavedFile>,
    onItemClick: (RecentSavedFile) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filteredFiles = if (query.isBlank()) {
        recentFiles
    } else {
        recentFiles.filter { it.name.contains(query, ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {

                    Text(
                        "Analysis History", 
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )
                },
//                actions = {
//                    IconButton(onClick = { }) {
//                        Icon(Icons.Default.FilterList, contentDescription = "Filter")
//                    }
//                },
//                colors = TopAppBarDefaults.topAppBarColors(
//                    containerColor = MaterialTheme.colorScheme.background,
//                    titleContentColor = MaterialTheme.colorScheme.onBackground
//                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // Search Bar
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                placeholder = { 
                    Text(
                        "Search reports...", 
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    ) 
                },
                leadingIcon = { 
                    Icon(
                        Icons.Default.Search, 
                        contentDescription = null, 
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    ) 
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                ),
                singleLine = true
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                item {
                    Text(
                        "Recent",
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (filteredFiles.isEmpty()) {
                    item {
                        EmptyHistoryState()
                    }
                } else {
                    itemsIndexed(filteredFiles) { index, recentFile ->
                        val relativeTime = formatRelativeTime(recentFile.savedAtEpochMs)
                        AnalysisHistoryItem(
                            name = recentFile.name,
                            date = "Saved $relativeTime",
                            status = "Saved",
                            statusColor = MaterialTheme.colorScheme.primary,
                            onClick = { onItemClick(recentFile) }
                        )
                        if (index < filteredFiles.lastIndex) {
                            Divider(
                                modifier = Modifier.padding(horizontal = 24.dp),
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyHistoryState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "No recent files",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Save a file in the editor to see it here.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatRelativeTime(epochMs: Long): String {
    val diffMs = System.currentTimeMillis() - epochMs
    return when {
        diffMs < 60_000L -> "just now"
        diffMs < 3_600_000L -> "${diffMs / 60_000L}m ago"
        diffMs < 86_400_000L -> "${diffMs / 3_600_000L}h ago"
        else -> "${diffMs / 86_400_000L}d ago"
    }
}
