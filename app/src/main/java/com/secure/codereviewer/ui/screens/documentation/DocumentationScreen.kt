package com.secure.codereviewer.ui.screens.documentation

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.secure.codereviewer.ui.theme.*

/**
 * DocumentationScreen:
 * A dedicated, clean, professional screen that displays the full analysis and optimization results:
 * - Direct Tab Row at the top without clutter.
 * - Unified stacked layout: Explanation on top, followed immediately by C++ Code.
 * - Clean light-mode design matching the application theme.
 * - Quick copy actions for individual sections or full report.
 * - One-tap action to return to the editor with the chosen code loaded.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentationScreen(
    explanation: String,
    commentedCode: String,
    optimizedCode: String? = null,
    optimizationVerified: Boolean = false,
    speedup: Double = 0.0,
    optimizationNote: String? = null,
    fileName: String = "analysis.cpp",
    onBack: UnitCallback,
    onOpenInEditor: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val hasOptimization = !optimizedCode.isNullOrBlank()
    val hasCommentedCode = commentedCode.isNotBlank()
    val bothAvailable = hasOptimization && hasCommentedCode && (optimizedCode != commentedCode)

    // By default, if optimization is present, show the optimized code; otherwise the commented code
    var isViewingCommented by remember { mutableStateOf(false) }

    val activeCode = remember(isViewingCommented, optimizedCode, commentedCode) {
        if (hasOptimization && (!isViewingCommented || !hasCommentedCode)) {
            optimizedCode ?: commentedCode
        } else {
            commentedCode.ifBlank { optimizedCode ?: "" }
        }
    }

    val tabs = remember { listOf("Overview", "Explanation", "Code") }
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = CppThemeTextPrimary
                        )
                    }
                },
                title = {
                    Column {
                        Text(
                            text = "Code Documentation",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = CppThemeTextPrimary
                        )
                        Text(
                            text = fileName,
                            style = MaterialTheme.typography.labelSmall,
                            color = CppThemeTextSecondary
                        )
                    }
                },
                actions = {
                    // Quick button to copy entire documentation (Explanation + Code)
                    IconButton(onClick = {
                        val fullReport = buildString {
                            appendLine("// ===========================================")
                            appendLine("// FILE: $fileName")
                            appendLine("// ===========================================")
                            appendLine()
                            appendLine("/*")
                            appendLine("EXPLANATION:")
                            appendLine(explanation.trim())
                            appendLine("*/")
                            appendLine()
                            appendLine(activeCode.trim())
                        }
                        clipboardManager.setText(AnnotatedString(fullReport))
                        Toast.makeText(context, "Full report copied to clipboard", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(
                            Icons.Outlined.ContentCopy,
                            contentDescription = "Copy Full Report",
                            tint = CppThemeTextSecondary
                        )
                    }

                    if (onOpenInEditor != null) {
                        IconButton(onClick = { onOpenInEditor(activeCode) }) {
                            Icon(
                                Icons.Outlined.Edit,
                                contentDescription = "Edit in IDE",
                                tint = CppThemeTextSecondary
                            )
                        }
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
            // Primary Navigation Tabs directly under top app bar (clean & modern)
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = CppThemeSurface,
                contentColor = CppThemePrimary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = CppThemePrimary,
                        height = 2.5.dp
                    )
                },
                divider = { Divider(color = CppThemeDivider) }
            ) {
                tabs.forEachIndexed { index, label ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (selectedTabIndex == index) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (selectedTabIndex == index) CppThemePrimary else CppThemeTextSecondary
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main Viewport
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                when (selectedTabIndex) {
                    0 -> OverviewTabView(
                        explanation = explanation,
                        activeCode = activeCode,
                        hasOptimization = hasOptimization,
                        optimizationVerified = optimizationVerified,
                        speedup = speedup,
                        bothAvailable = bothAvailable,
                        isViewingCommented = isViewingCommented,
                        onToggleCodeView = { isViewingCommented = it }
                    )
                    1 -> ExplanationTabView(explanation = explanation)
                    2 -> CodeOnlyTabView(
                        code = activeCode,
                        hasOptimization = hasOptimization,
                        optimizationVerified = optimizationVerified,
                        speedup = speedup,
                        bothAvailable = bothAvailable,
                        isViewingCommented = isViewingCommented,
                        onToggleCodeView = { isViewingCommented = it }
                    )
                }
            }

            // Bottom sticky action: Open in Editor
            if (onOpenInEditor != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = CppThemeSurface,
                    border = BorderStroke(1.dp, CppThemeDivider)
                ) {
                    Button(
                        onClick = { onOpenInEditor(activeCode) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                            .height(50.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CppThemePrimary,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Open in Code Editor",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

typealias UnitCallback = () -> Unit

/**
 * Tab 0: Overview (Web-Style Stacked View)
 * Displays Explanation first at the top, then C++ Code block right underneath.
 * Clean, light-themed developer card styling.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OverviewTabView(
    explanation: String,
    activeCode: String,
    hasOptimization: Boolean,
    optimizationVerified: Boolean,
    speedup: Double,
    bothAvailable: Boolean,
    isViewingCommented: Boolean,
    onToggleCodeView: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section 1: Explanation
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Outlined.Description,
                        contentDescription = null,
                        tint = CppThemePrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Explanation",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = CppThemeTextPrimary
                    )
                }
                DocCopyButton(textToCopy = explanation, label = "Copy")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                color = CppThemeSurface,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, CppThemeDivider),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = explanation.ifBlank { "No explanation generated for this snippet." },
                    style = MaterialTheme.typography.bodyMedium,
                    color = CppThemeTextPrimary,
                    lineHeight = 24.sp,
                    modifier = Modifier.padding(18.dp)
                )
            }
        }

        // Section 2: Code block (Light-themed developer code card)
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Outlined.Code,
                        contentDescription = null,
                        tint = CppThemePrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = if (hasOptimization && !isViewingCommented) "Optimized C++" else "Commented C++",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = CppThemeTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (hasOptimization && !isViewingCommented && optimizationVerified) {
                        Surface(
                            color = CppThemeSuccessSubtle,
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, CppThemeSuccess.copy(alpha = 0.25f))
                        ) {
                            Text(
                                text = if (speedup > 1.0) "⚡ ${speedup}x" else "✓ Verified",
                                color = CppThemeSuccess,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                DocCopyButton(textToCopy = activeCode, label = "Copy")
            }

            if (bothAvailable) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilterChip(
                        selected = !isViewingCommented,
                        onClick = { onToggleCodeView(false) },
                        label = { Text("⚡ Optimized Code", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CppThemePrimarySubtle,
                            selectedLabelColor = CppThemePrimary
                        )
                    )
                    FilterChip(
                        selected = isViewingCommented,
                        onClick = { onToggleCodeView(true) },
                        label = { Text("📝 Original Commented", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CppThemePrimarySubtle,
                            selectedLabelColor = CppThemePrimary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                color = CppThemeSurfaceMuted,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, CppThemeDivider),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = activeCode.ifBlank { "// No code available." },
                    color = CppThemeTextPrimary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.5.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier
                        .padding(18.dp)
                        .horizontalScroll(rememberScrollState())
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * Tab 1: Detailed Explanation Only View
 */
@Composable
private fun ExplanationTabView(explanation: String) {
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Outlined.Description,
                    contentDescription = null,
                    tint = CppThemePrimary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Detailed Explanation",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = CppThemeTextPrimary
                )
            }
            DocCopyButton(textToCopy = explanation, label = "Copy")
        }
        Spacer(modifier = Modifier.height(10.dp))
        Surface(
            color = CppThemeSurface,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, CppThemeDivider),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = explanation.ifBlank { "No explanation generated for this snippet." },
                style = MaterialTheme.typography.bodyMedium,
                color = CppThemeTextPrimary,
                lineHeight = 24.sp,
                modifier = Modifier.padding(18.dp)
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
    }
}

/**
 * Tab 2: Code Only View (Light Theme)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CodeOnlyTabView(
    code: String,
    hasOptimization: Boolean,
    optimizationVerified: Boolean,
    speedup: Double,
    bothAvailable: Boolean,
    isViewingCommented: Boolean,
    onToggleCodeView: (Boolean) -> Unit
) {
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
            Row(
                modifier = Modifier.weight(1f, fill = false),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Outlined.Code,
                    contentDescription = null,
                    tint = CppThemePrimary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = if (hasOptimization && !isViewingCommented) "Optimized C++" else "Commented C++",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = CppThemeTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (hasOptimization && !isViewingCommented && optimizationVerified) {
                    Surface(
                        color = CppThemeSuccessSubtle,
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, CppThemeSuccess.copy(alpha = 0.25f))
                    ) {
                        Text(
                            text = if (speedup > 1.0) "⚡ ${speedup}x" else "✓ Verified",
                            color = CppThemeSuccess,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            DocCopyButton(textToCopy = code, label = "Copy")
        }

        if (bothAvailable) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                FilterChip(
                    selected = !isViewingCommented,
                    onClick = { onToggleCodeView(false) },
                    label = { Text("⚡ Optimized Code", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CppThemePrimarySubtle,
                        selectedLabelColor = CppThemePrimary
                    )
                )
                FilterChip(
                    selected = isViewingCommented,
                    onClick = { onToggleCodeView(true) },
                    label = { Text("📝 Original Commented", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CppThemePrimarySubtle,
                        selectedLabelColor = CppThemePrimary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Surface(
            color = CppThemeSurfaceMuted,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, CppThemeDivider),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = code.ifBlank { "// No code available." },
                color = CppThemeTextPrimary,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.5.sp,
                lineHeight = 20.sp,
                modifier = Modifier
                    .padding(18.dp)
                    .horizontalScroll(rememberScrollState())
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
    }
}

/**
 * Tactile copy button with toast feedback.
 * Guaranteed never to wrap or squish vertically.
 */
@Composable
private fun DocCopyButton(textToCopy: String, label: String = "Copy") {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    OutlinedButton(
        onClick = {
            if (textToCopy.isNotBlank()) {
                clipboardManager.setText(AnnotatedString(textToCopy))
                Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
            }
        },
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, CppThemeDivider),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = CppThemeTextPrimary),
        modifier = Modifier.height(32.dp)
    ) {
        Icon(
            Icons.Outlined.ContentCopy,
            contentDescription = null,
            modifier = Modifier.size(13.dp),
            tint = CppThemeTextSecondary
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            softWrap = false
        )
    }
}
