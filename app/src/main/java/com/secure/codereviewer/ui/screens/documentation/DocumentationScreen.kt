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
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Speed
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.secure.codereviewer.ui.theme.*

/**
 * DocumentationScreen:
 * A dedicated, professional screen that displays the full analysis results:
 * - Clean overview header with verified badges.
 * - Interactive Segmented Tabs:
 *     1. Overview (Explanation + Key points)
 *     2. Commented Code (Documented C++ source)
 *     3. Side-by-Side (Compare explanation with code)
 *     4. Optimized Loop (If loop optimization was generated)
 * - Quick copy actions for individual sections or full report.
 * - One-tap action to return to the editor.
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
    onBack: () -> Unit,
    onOpenInEditor: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val hasOptimization = !optimizedCode.isNullOrBlank()
    val tabs = remember(hasOptimization) {
        val list = mutableListOf("Explanation", "Commented Code", "Split View")
        if (hasOptimization) {
            list.add("Optimized Loop")
        }
        list
    }

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
                            appendLine(commentedCode.trim())
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
                        IconButton(onClick = { onOpenInEditor(commentedCode) }) {
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
            // Top metadata card
            MetadataHeaderCard(
                fileName = fileName,
                hasOptimization = hasOptimization,
                optimizationVerified = optimizationVerified,
                speedup = speedup,
                optimizationNote = optimizationNote
            )

            // Primary Navigation Tabs
            TabRow(
                selectedTabIndex = selectedTabIndex.coerceAtMost(tabs.size - 1),
                containerColor = CppThemeSurface,
                contentColor = CppThemePrimary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex.coerceAtMost(tabs.size - 1)]),
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
                    0 -> ExplanationTabView(explanation = explanation)
                    1 -> CommentedCodeTabView(code = commentedCode)
                    2 -> SplitTabView(explanation = explanation, code = commentedCode)
                    3 -> if (hasOptimization) {
                        OptimizedLoopTabView(
                            code = optimizedCode ?: "",
                            verified = optimizationVerified,
                            speedup = speedup
                        )
                    }
                }
            }

            // Bottom action: Open in Editor
            if (onOpenInEditor != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = CppThemeSurface,
                    border = BorderStroke(1.dp, CppThemeDivider)
                ) {
                    Button(
                        onClick = { onOpenInEditor(commentedCode) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                            .height(48.dp),
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

/**
 * Top summary card showing file type, analysis tags, and verification status.
 */
@Composable
private fun MetadataHeaderCard(
    fileName: String,
    hasOptimization: Boolean,
    optimizationVerified: Boolean,
    speedup: Double,
    optimizationNote: String? = null
) {
    Surface(
        color = CppThemeSurface,
        border = BorderStroke(1.dp, CppThemeDivider),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        color = CppThemePrimarySubtle,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Outlined.Description,
                                contentDescription = null,
                                tint = CppThemePrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "Analysis Ready",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = CppThemeTextPrimary
                        )
                        Text(
                            text = "AST verified comments and semantic breakdown",
                            style = MaterialTheme.typography.labelSmall,
                            color = CppThemeTextSecondary
                        )
                    }
                }

                // Status chip
                if (hasOptimization && optimizationVerified) {
                    Surface(
                        color = CppThemeSuccessSubtle,
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, CppThemeSuccess.copy(alpha = 0.25f))
                    ) {
                        Text(
                            text = if (speedup > 1.0) "${speedup}x Faster" else "Verified",
                            color = CppThemeSuccess,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                } else {
                    Surface(
                        color = CppThemePrimarySubtle,
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, CppThemePrimary.copy(alpha = 0.25f))
                    ) {
                        Text(
                            text = "Documented",
                            color = CppThemePrimary,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            if (!optimizationNote.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = CppThemeSurfaceMuted,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Optimization: $optimizationNote",
                        style = MaterialTheme.typography.labelSmall,
                        color = CppThemeTextSecondary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

/**
 * Tab 1: Detailed Explanation View
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
            Text(
                text = "Detailed Explanation",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = CppThemeTextPrimary
            )
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
 * Tab 2: Documented C++ Code View
 */
@Composable
private fun CommentedCodeTabView(code: String) {
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
                text = "Commented Source Code",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = CppThemeTextPrimary
            )
            DocCopyButton(textToCopy = code, label = "Copy Code")
        }
        Spacer(modifier = Modifier.height(10.dp))
        Surface(
            color = CppThemeEditorDark,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = code.ifBlank { "// No commented code available." },
                color = Color(0xFFE2E8F0),
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
 * Tab 3: Split View (Both explanation & code on one scrollable screen)
 */
@Composable
private fun SplitTabView(explanation: String, code: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section 1: Explanation
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "1. Code Logic & Summary",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = CppThemeTextPrimary
                )
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
                    text = explanation.ifBlank { "No explanation available." },
                    style = MaterialTheme.typography.bodyMedium,
                    color = CppThemeTextPrimary,
                    lineHeight = 22.sp,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        // Section 2: Code
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "2. Commented C++ Implementation",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = CppThemeTextPrimary
                )
                DocCopyButton(textToCopy = code, label = "Copy Code")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                color = CppThemeEditorDark,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = code.ifBlank { "// No code available." },
                    color = Color(0xFFE2E8F0),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    lineHeight = 19.sp,
                    modifier = Modifier
                        .padding(16.dp)
                        .horizontalScroll(rememberScrollState())
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

/**
 * Tab 4: Optimized Loop View (if recursion was rewritten)
 */
@Composable
private fun OptimizedLoopTabView(
    code: String,
    verified: Boolean,
    speedup: Double
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
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Iterative Loop Rewrite",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = CppThemeTextPrimary
                )
                if (verified) {
                    Surface(
                        color = CppThemeSuccessSubtle,
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, CppThemeSuccess.copy(alpha = 0.25f))
                    ) {
                        Text(
                            text = if (speedup > 1.0) "${speedup}x Faster" else "Verified Equivalent",
                            color = CppThemeSuccess,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            DocCopyButton(textToCopy = code, label = "Copy Loop")
        }
        Spacer(modifier = Modifier.height(10.dp))
        Surface(
            color = CppThemeEditorDark,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = code.ifBlank { "// No loop rewrite available." },
                color = Color(0xFFE2E8F0),
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
 */
@Composable
private fun DocCopyButton(textToCopy: String, label: String) {
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
        border = BorderStroke(1.dp, CppThemeDivider),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = CppThemeTextPrimary)
    ) {
        Icon(
            Icons.Outlined.ContentCopy,
            contentDescription = null,
            modifier = Modifier.size(13.dp),
            tint = CppThemeTextSecondary
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium
        )
    }
}
