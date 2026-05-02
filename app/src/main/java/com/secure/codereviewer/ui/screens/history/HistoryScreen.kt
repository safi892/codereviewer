package com.secure.codereviewer.ui.screens.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.secure.codereviewer.ui.components.AnalysisHistoryItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onItemClick: (HistoryUiItem) -> Unit
) {
    val viewModel: HistoryViewModel = viewModel()
    val listState = rememberLazyListState()
    var query by remember { mutableStateOf("") }
    val filteredItems = if (query.isBlank()) {
        viewModel.items
    } else {
        viewModel.items.filter { item ->
            item.title.contains(query, ignoreCase = true) ||
                item.inputCode.contains(query, ignoreCase = true)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadInitial()
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
                actions = {
                    IconButton(
                        onClick = { viewModel.refresh() },
                        enabled = !viewModel.isLoading
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reload")
                    }
                },
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
                state = listState,
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

                when {
                    viewModel.isLoading && viewModel.items.isEmpty() -> {
                        item {
                            HistoryLoadingState()
                        }
                    }
                    viewModel.errorMessage != null && viewModel.items.isEmpty() -> {
                        item {
                            HistoryErrorState(
                                message = viewModel.errorMessage ?: "Failed to load history",
                                onRetry = { viewModel.refresh() }
                            )
                        }
                    }
                    filteredItems.isEmpty() -> {
                        item {
                            EmptyHistoryState(query)
                        }
                    }
                    else -> {
                        itemsIndexed(filteredItems) { index, historyItem ->
                            val relativeTime = formatRelativeTime(historyItem.createdAtEpochMs)
                            val sourceLabel = historyItem.source
                                ?.replaceFirstChar { it.uppercase() }
                            val dateLabel = if (sourceLabel.isNullOrBlank()) {
                                "Analyzed $relativeTime"
                            } else {
                                "From $sourceLabel - $relativeTime"
                            }
                            AnalysisHistoryItem(
                                name = historyItem.title,
                                date = dateLabel,
                                status = "Analyzed",
                                statusColor = MaterialTheme.colorScheme.primary,
                                onClick = { onItemClick(historyItem) }
                            )
                            if (index < filteredItems.lastIndex) {
                                Divider(
                                    modifier = Modifier.padding(horizontal = 24.dp),
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                )
                            }

                            if (index >= filteredItems.lastIndex - 2) {
                                viewModel.loadMore()
                            }
                        }

                        if (viewModel.isLoading && viewModel.items.isNotEmpty()) {
                            item {
                                LoadingMoreRow()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyHistoryState(query: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = if (query.isBlank()) "No history yet" else "No matches",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = if (query.isBlank()) {
                "Run an analysis to see it here."
            } else {
                "Try a different search."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun HistoryLoadingState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun LoadingMoreRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(strokeWidth = 2.dp)
    }
}

@Composable
private fun HistoryErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Could not load history",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontStyle = FontStyle.Italic
        )
        OutlinedButton(onClick = onRetry) {
            Text("Retry")
        }
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
