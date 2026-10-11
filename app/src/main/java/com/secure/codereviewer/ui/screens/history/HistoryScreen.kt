package com.secure.codereviewer.ui.screens.history

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.secure.codereviewer.ui.components.AnalysisHistoryItem
import com.secure.codereviewer.ui.theme.*

/**
 * HistoryScreen:
 * A clean, professional log of past code analyses.
 * - Search by title or snippet keyword.
 * - Smooth infinite scroll.
 * - Tabbed modal sheet to inspect explanation and code side-by-side.
 * - Quick copy & editor handoff.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onItemClick: (HistoryUiItem) -> Unit
) {
    val viewModel: HistoryViewModel = viewModel()
    val listState = rememberLazyListState()
    var query by remember { mutableStateOf("") }
    var selectedItemForDetail by remember { mutableStateOf<HistoryUiItem?>(null) }

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
            TopAppBar(
                title = {
                    Text(
                        "Analysis History",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = CppThemeTextPrimary
                    )
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.refresh() },
                        enabled = !viewModel.isLoading
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = if (viewModel.isLoading) CppThemeTextTertiary else CppThemeTextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CppThemeSurface,
                    titleContentColor = CppThemeTextPrimary
                )
            )
        },
        containerColor = CppThemeBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // Minimalist Search Input
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                placeholder = {
                    Text(
                        "Search reports or functions...",
                        color = CppThemeTextTertiary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = CppThemeTextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = CppThemeSurface,
                    unfocusedContainerColor = CppThemeSurface,
                    focusedBorderColor = CppThemePrimary,
                    unfocusedBorderColor = CppThemeDivider
                ),
                singleLine = true
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                when {
                    viewModel.isLoading && viewModel.items.isEmpty() -> {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 60.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = CppThemePrimary, strokeWidth = 2.dp)
                            }
                        }
                    }
                    viewModel.errorMessage != null && viewModel.items.isEmpty() -> {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "Unable to load history",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = CppThemeTextPrimary
                                )
                                Text(
                                    text = viewModel.errorMessage ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CppThemeTextSecondary,
                                    textAlign = TextAlign.Center
                                )
                                OutlinedButton(
                                    onClick = { viewModel.refresh() },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Retry", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                    filteredItems.isEmpty() -> {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 60.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (query.isBlank()) "No reviews yet. Analyze code to save history." else "No matches found.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = CppThemeTextSecondary
                                )
                            }
                        }
                    }
                    else -> {
                        itemsIndexed(filteredItems) { index, historyItem ->
                            val relativeTime = formatRelativeTime(historyItem.createdAtEpochMs)
                            val sourceLabel = historyItem.source?.replaceFirstChar { it.uppercase() } ?: "Android"

                            AnalysisHistoryItem(
                                name = historyItem.title,
                                date = "$sourceLabel · $relativeTime",
                                status = "Verified",
                                statusColor = CppThemeSuccess,
                                onClick = { selectedItemForDetail = historyItem }
                            )

                            if (index < filteredItems.lastIndex) {
                                Divider(
                                    modifier = Modifier.padding(horizontal = 20.dp),
                                    color = CppThemeDivider.copy(alpha = 0.6f)
                                )
                            }

                            if (index >= filteredItems.lastIndex - 2) {
                                viewModel.loadMore()
                            }
                        }

                        if (viewModel.isLoading && viewModel.items.isNotEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Inspection Sheet
    if (selectedItemForDetail != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedItemForDetail = null },
            containerColor = CppThemeSurface,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
            dragHandle = { BottomSheetDefaults.DragHandle(color = CppThemeDivider) }
        ) {
            HistoryInspectionSheet(
                item = selectedItemForDetail!!,
                onOpenInEditor = {
                    val item = selectedItemForDetail!!
                    selectedItemForDetail = null
                    onItemClick(item)
                },
                onClose = { selectedItemForDetail = null }
            )
        }
    }
}

/**
 * Inspection sheet with side-by-side tabs: Explanation vs Commented Code
 */
@Composable
private fun HistoryInspectionSheet(
    item: HistoryUiItem,
    onOpenInEditor: () -> Unit,
    onClose: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Explanation", "Commented Code", "Raw Source")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.88f)
            .padding(bottom = 20.dp)
    ) {
        // Sheet Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = CppThemeTextPrimary
                )
                Text(
                    text = "ID: #${item.id} · ${formatRelativeTime(item.createdAtEpochMs)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = CppThemeTextSecondary
                )
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = CppThemeTextSecondary)
            }
        }

        // Tab Row
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = CppThemeSurface,
            contentColor = CppThemePrimary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = CppThemePrimary,
                    height = 2.5.dp
                )
            },
            divider = { Divider(color = CppThemeDivider) }
        ) {
            tabs.forEachIndexed { index, label ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (selectedTab == index) CppThemePrimary else CppThemeTextSecondary
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Content
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            when (selectedTab) {
                0 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Semantic Explanation",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = CppThemeTextPrimary
                            )
                            HistoryCopyPill(textToCopy = item.explanation, label = "Copy")
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            color = CppThemeSurfaceMuted,
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CppThemeDivider),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = item.explanation.ifBlank { "No explanation recorded." },
                                style = MaterialTheme.typography.bodyMedium,
                                color = CppThemeTextPrimary,
                                modifier = Modifier.padding(18.dp),
                                lineHeight = 24.sp
                            )
                        }
                    }
                }
                1 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Commented Source",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = CppThemeTextPrimary
                            )
                            HistoryCopyPill(textToCopy = item.commentedCode, label = "Copy Code")
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            color = CppThemeEditorDark,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = item.commentedCode.ifBlank { "// No commented code" },
                                color = Color(0xFFE2E8F0),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.5.sp,
                                lineHeight = 20.sp,
                                modifier = Modifier
                                    .padding(18.dp)
                                    .horizontalScroll(rememberScrollState())
                            )
                        }
                    }
                }
                2 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Raw Input Code",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = CppThemeTextPrimary
                            )
                            HistoryCopyPill(textToCopy = item.inputCode, label = "Copy")
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            color = CppThemeEditorDark,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = item.inputCode.ifBlank { "// No input code recorded" },
                                color = Color(0xFFE2E8F0),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.5.sp,
                                lineHeight = 20.sp,
                                modifier = Modifier
                                    .padding(18.dp)
                                    .horizontalScroll(rememberScrollState())
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Open in Editor Button
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = CppThemeSurface
        ) {
            Button(
                onClick = onOpenInEditor,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .height(48.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CppThemePrimary,
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.Outlined.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Open in IDE Editor", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun HistoryCopyPill(textToCopy: String, label: String) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    OutlinedButton(
        onClick = {
            if (textToCopy.isNotBlank()) {
                clipboardManager.setText(AnnotatedString(textToCopy))
                Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
            }
        },
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CppThemeDivider),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = CppThemeTextPrimary)
    ) {
        Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(13.dp), tint = CppThemeTextSecondary)
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium)
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
