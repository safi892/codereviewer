package com.secure.codereviewer.ui.screens.editor

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.secure.codereviewer.ui.theme.*
import com.secure.codereviewer.util.EditorSetup
import io.github.rosemoe.sora.widget.CodeEditor
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce

private const val EDITOR_SYNC_DEBOUNCE_MS = 250L
private const val DEFAULT_SNIPPET =
    "// C++ Source Code\n#include <iostream>\n\nint main() {\n    std::cout << \"Hello World!\" << std::endl;\n    return 0;\n}"

/**
 * EditorScreen:
 * A high-craft, professional IDE workspace for C++ review.
 * Features:
 * - Minimalist, high-utility top bar with clean file metadata.
 * - Subdued, informative syntax validation banner with one-click token repair.
 * - Inline recursive-to-loop optimization switch.
 * - Polished bottom results sheet with custom tab navigation and copy actions.
 */
@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
fun EditorScreen(
    onBack: () -> Unit,
    onFileSaved: (String) -> Unit,
    initialContent: String? = null,
    initialFileName: String? = null,
    initialContentKey: Int = 0,
    onInitialContentConsumed: () -> Unit = {},
    onOpenDocumentation: ((explanation: String, commentedCode: String, optCode: String?, optVerified: Boolean, speedup: Double, optNote: String?, fileName: String) -> Unit)? = null
) {
    val context = LocalContext.current
    val resolver = context.contentResolver
    val editorViewModel: EditorViewModel = viewModel()
    var editorRef by remember { mutableStateOf<CodeEditor?>(null) }

    // Typing fires ContentChangeEvent on every keystroke. We only emit a cheap signal here
    // and take ONE text snapshot after the user pauses, instead of copying the whole
    // document + writing Compose state + recomposing the screen on every key press.
    val editorChanges = remember {
        MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    }

    val openFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) editorViewModel.loadFile(uri, resolver)
    }

    val saveFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/x-c++src")
    ) { uri: Uri? ->
        if (uri != null) {
            editorViewModel.syncFromEditor(editorRef?.text?.toString())
            editorViewModel.saveToUri(uri, resolver) { ok ->
                if (ok) onFileSaved(editorViewModel.currentFileName)
            }
        }
    }

    LaunchedEffect(initialContentKey) {
        if (initialContent != null) {
            editorViewModel.openContent(
                content = initialContent,
                fileName = initialFileName ?: "main.cpp"
            )
            onInitialContentConsumed()
        }
    }

    // Debounced editor -> ViewModel sync.
    LaunchedEffect(editorChanges) {
        editorChanges
            .debounce(EDITOR_SYNC_DEBOUNCE_MS)
            .collect {
                editorRef?.takeIf { !it.isReleased }?.let { editor ->
                    editorViewModel.onEditorTextChanged(editor.text.toString())
                }
            }
    }

    // ViewModel -> editor, only when the VM replaced the text (open/fix/new file).
    LaunchedEffect(editorViewModel.externalContentVersion, editorRef) {
        val editor = editorRef ?: return@LaunchedEffect
        if (editorViewModel.externalContentVersion == 0) return@LaunchedEffect
        editor.setText(editorViewModel.currentContent)
    }

    // Heavy TextMate language creation happens off the main thread, then is applied here.
    LaunchedEffect(editorRef) {
        val editor = editorRef ?: return@LaunchedEffect
        val language = EditorSetup.createLanguage(context)
        EditorSetup.applyTo(editor, language)
    }

    LaunchedEffect(editorViewModel.analysisError) {
        editorViewModel.analysisError?.let { err ->
            Toast.makeText(context, err, Toast.LENGTH_LONG).show()
        }
    }

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
                    // Filename + save status stacked vertically. A side-by-side chip got
                    // squeezed by the 4 action icons and wrapped one letter per line.
                    val isSaved = editorViewModel.currentUri != null
                    val statusColor = if (isSaved) CppThemeSuccess else CppThemeWarning
                    Column(verticalArrangement = Arrangement.Center) {
                        Text(
                            editorViewModel.currentFileName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = CppThemeTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(statusColor)
                            )
                            Text(
                                text = if (isSaved) "Saved" else "Unsaved",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = statusColor,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                },
                actions = {
                    // Icon + small caption so each action's purpose is obvious.
                    LabeledTopBarAction(
                        icon = Icons.Default.Add,
                        label = "New",
                        onClick = { editorViewModel.newFile() }
                    )
                    LabeledTopBarAction(
                        icon = Icons.Default.FolderOpen,
                        label = "Open",
                        onClick = { openFileLauncher.launch(arrayOf("text/plain", "text/x-c++src")) }
                    )
                    LabeledTopBarAction(
                        icon = Icons.Default.Save,
                        label = "Save",
                        enabled = editorViewModel.currentUri != null,
                        onClick = {
                            editorViewModel.syncFromEditor(editorRef?.text?.toString())
                            editorViewModel.saveCurrentFile(resolver) { ok ->
                                if (ok) onFileSaved(editorViewModel.currentFileName)
                            }
                        }
                    )
                    LabeledTopBarAction(
                        icon = Icons.Default.SaveAs,
                        label = "Save As",
                        onClick = {
                            editorViewModel.syncFromEditor(editorRef?.text?.toString())
                            saveFileLauncher.launch(editorViewModel.currentFileName)
                        }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
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
            // Live Syntax Verification Bar
            SyntaxValidationBar(
                isValidating = editorViewModel.isValidating,
                isValid = editorViewModel.isSyntaxValid,
                message = editorViewModel.validationMessage,
                hasFix = editorViewModel.currentSuggestedFix != null,
                onApplyFix = { editorViewModel.applySuggestedFix() }
            )

            // Sora Editor Container
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(CppThemeEditorDark)
            ) {
                AndroidView(
                    factory = { ctx ->
                        CodeEditor(ctx).apply {
                            // Only cheap view setup here; grammar/language load is async (see LaunchedEffect above).
                            EditorSetup.applyBaseSettings(this)
                            val initial = editorViewModel.currentContent.ifEmpty { DEFAULT_SNIPPET }
                            setText(initial)
                            // Keep VM in sync with what's shown (Analyze button needs non-blank content).
                            editorViewModel.syncFromEditor(initial)
                            subscribeEvent(io.github.rosemoe.sora.event.ContentChangeEvent::class.java) { _, _ ->
                                editorChanges.tryEmit(Unit)
                            }
                            editorRef = this
                        }
                    },
                    onRelease = { editor ->
                        // Stops Sora's background analyzer thread; avoids leaking the view.
                        editorRef = null
                        editor.release()
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Bottom Action & Options Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = CppThemeSurface,
                tonalElevation = 2.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, CppThemeDivider)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Optimization switch row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // weight(1f): the label takes the remaining space and wraps,
                        // instead of pushing the Switch off-screen.
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Speed,
                                contentDescription = null,
                                tint = if (editorViewModel.isOptimizationEnabled) CppThemePrimary else CppThemeTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text(
                                    text = "Recursion to Loop Optimizer",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = CppThemeTextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Rewrites recursive functions into loops with AST proof",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CppThemeTextTertiary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Switch(
                            checked = editorViewModel.isOptimizationEnabled,
                            onCheckedChange = { editorViewModel.isOptimizationEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = CppThemePrimary,
                                uncheckedThumbColor = CppThemeTextSecondary,
                                uncheckedTrackColor = CppThemeSurfaceMuted,
                                uncheckedBorderColor = CppThemeTextTertiary
                            )
                        )
                    }

                    // Action Buttons Row: Separate Analyze and Optimize
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Optimize Button (direct optimization without full analysis)
                        OutlinedButton(
                            onClick = {
                                editorViewModel.syncFromEditor(editorRef?.text?.toString())
                                editorViewModel.optimizeCurrentCode { opt ->
                                    if (onOpenDocumentation != null) {
                                        onOpenDocumentation(
                                            opt.explanation,
                                            opt.code,
                                            opt.code,
                                            opt.verified,
                                            opt.speedup,
                                            opt.note,
                                            editorViewModel.currentFileName
                                        )
                                    } else {
                                        editorViewModel.showResultsSheet = true
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            // Default padding is 24dp per side, which left no room for the label.
                            contentPadding = PaddingValues(horizontal = 12.dp),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CppThemePrimary),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = CppThemePrimary
                            ),
                            enabled = !editorViewModel.isAnalyzing && !editorViewModel.isOptimizing && editorViewModel.currentContent.isNotBlank()
                        ) {
                            if (editorViewModel.isOptimizing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = CppThemePrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Optimizing...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            } else {
                                Icon(
                                    Icons.Outlined.Speed,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Optimize",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Primary Run Analysis Action
                        Button(
                            onClick = {
                                editorViewModel.syncFromEditor(editorRef?.text?.toString())
                                editorViewModel.analyzeCurrentCode { result, opt ->
                                    if (onOpenDocumentation != null) {
                                        val finalExplanation = if (result.explanation.isNotBlank()) {
                                            result.explanation
                                        } else {
                                            opt?.explanation ?: ""
                                        }
                                        onOpenDocumentation(
                                            finalExplanation,
                                            result.commented_code,
                                            opt?.code,
                                            opt?.verified ?: false,
                                            opt?.speedup ?: 0.0,
                                            opt?.note,
                                            editorViewModel.currentFileName
                                        )
                                    } else {
                                        editorViewModel.showResultsSheet = true
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CppThemePrimary,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            enabled = !editorViewModel.isAnalyzing && !editorViewModel.isOptimizing && editorViewModel.currentContent.isNotBlank()
                        ) {
                            if (editorViewModel.isAnalyzing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    "Analyzing...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            } else {
                                Icon(
                                    Icons.Outlined.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    "Analyze Code",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // High-craft Bottom Sheet Modal for Results
    if (editorViewModel.showResultsSheet) {
        ModalBottomSheet(
            onDismissRequest = { editorViewModel.showResultsSheet = false },
            containerColor = CppThemeSurface,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
            dragHandle = { BottomSheetDefaults.DragHandle(color = CppThemeDivider) }
        ) {
            ResultsInspectionSheet(
                viewModel = editorViewModel,
                onClose = { editorViewModel.showResultsSheet = false },
                onOpenDocumentation = onOpenDocumentation
            )
        }
    }
}

/**
 * Top bar action with a small caption under the icon.
 * 48dp min touch target; the caption doubles as the accessibility label.
 */
@Composable
private fun LabeledTopBarAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    val tint = if (enabled) CppThemeTextSecondary else CppThemeTextTertiary
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            lineHeight = 12.sp,
            fontWeight = FontWeight.Medium,
            color = tint,
            maxLines = 1,
            softWrap = false
        )
    }
}

/**
 * Syntax validation feedback bar:
 * Unobtrusive, calm status line with auto-fix pill.
 */
@Composable
private fun SyntaxValidationBar(
    isValidating: Boolean,
    isValid: Boolean,
    message: String?,
    hasFix: Boolean,
    onApplyFix: () -> Unit
) {
    AnimatedVisibility(
        visible = isValidating || message != null,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        val (bgColor, textColor, icon) = when {
            isValidating -> Triple(CppThemeSurfaceMuted, CppThemeTextSecondary, null)
            isValid -> Triple(CppThemeSuccessSubtle, CppThemeSuccess, Icons.Default.CheckCircle)
            else -> Triple(CppThemeErrorSubtle, CppThemeError, Icons.Default.ErrorOutline)
        }

        Surface(
            color = bgColor,
            modifier = Modifier.fillMaxWidth(),
            border = androidx.compose.foundation.BorderStroke(1.dp, CppThemeDivider.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isValidating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(13.dp),
                        strokeWidth = 2.dp,
                        color = textColor
                    )
                } else if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = textColor,
                        modifier = Modifier.size(15.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isValidating) "Validating C++ syntax..." else (message ?: "Syntax OK"),
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                    maxLines = 2
                )
                if (hasFix) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onApplyFix,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CppThemePrimary,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Auto-Fix", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

/**
 * Results Inspection Sheet:
 * Clean, tabbed review modal matching modern code inspection tools.
 */
@Composable
private fun ResultsInspectionSheet(
    viewModel: EditorViewModel,
    onClose: () -> Unit,
    onOpenDocumentation: ((explanation: String, commentedCode: String, optCode: String?, optVerified: Boolean, speedup: Double, optNote: String?, fileName: String) -> Unit)? = null
) {
    val opt = viewModel.optimizationResult
    val hasOpt = opt != null && (opt.changed || opt.code.isNotBlank())
    val hasAnalysis = viewModel.lastExplanation != null || viewModel.lastCommentedCode != null

    val tabList = mutableListOf<String>()
    if (hasAnalysis) {
        tabList.add("Explanation")
        tabList.add("Commented Code")
    }
    if (hasOpt) {
        tabList.add("Optimized Code")
    }
    if (tabList.isEmpty()) {
        tabList.add("Results")
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.88f)
            .padding(bottom = 20.dp)
    ) {
        // Modal Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Review Summary",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = CppThemeTextPrimary
                )
                Text(
                    text = if (viewModel.needsReview) "Human review suggested" else "Analysis verified",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (viewModel.needsReview) CppThemeWarning else CppThemeTextSecondary
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onOpenDocumentation != null && viewModel.lastExplanation != null && viewModel.lastCommentedCode != null) {
                    IconButton(onClick = {
                        onClose()
                        onOpenDocumentation(
                            viewModel.lastExplanation ?: "",
                            viewModel.lastCommentedCode ?: "",
                            viewModel.optimizationResult?.code,
                            viewModel.optimizationResult?.verified ?: false,
                            viewModel.optimizationResult?.speedup ?: 0.0,
                            viewModel.optimizationResult?.note,
                            viewModel.currentFileName
                        )
                    }) {
                        Icon(
                            Icons.Default.Fullscreen,
                            contentDescription = "Open Full Screen",
                            tint = CppThemePrimary
                        )
                    }
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = CppThemeTextSecondary)
                }
            }
        }

        // Custom Minimalist Tab Bar
        TabRow(
            selectedTabIndex = viewModel.selectedResultTab.coerceAtMost(tabList.size - 1),
            containerColor = CppThemeSurface,
            contentColor = CppThemePrimary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[viewModel.selectedResultTab.coerceAtMost(tabList.size - 1)]),
                    color = CppThemePrimary,
                    height = 2.5.dp
                )
            },
            divider = { Divider(color = CppThemeDivider) }
        ) {
            tabList.forEachIndexed { index, label ->
                Tab(
                    selected = viewModel.selectedResultTab == index,
                    onClick = { viewModel.selectedResultTab = index },
                    text = {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (viewModel.selectedResultTab == index) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (viewModel.selectedResultTab == index) CppThemePrimary else CppThemeTextSecondary
                        )
                    }
                )
            }
        }

        // When loop optimization was requested but code is already iterative / not recursive,
        // show an explicit informative pill so the user knows why code wasn't rewritten.
        if (viewModel.isOptimizationEnabled && opt != null && !opt.changed) {
            Surface(
                color = CppThemePrimarySubtle,
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CppThemePrimary.copy(alpha = 0.2f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = CppThemePrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = opt.note.ifBlank { "No recursion detected. Code already uses iterative loops." },
                        style = MaterialTheme.typography.bodySmall,
                        color = CppThemePrimary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        val activeTab = tabList.getOrNull(viewModel.selectedResultTab.coerceAtMost(tabList.size - 1)) ?: "Explanation"

        // Content viewport
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            when (activeTab) {
                "Explanation" -> {
                    // TAB: Explanation
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
                            ModernCopyButton(textToCopy = viewModel.lastExplanation ?: "", label = "Copy")
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            color = CppThemeSurfaceMuted,
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CppThemeDivider),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = viewModel.lastExplanation ?: "No explanation returned.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = CppThemeTextPrimary,
                                modifier = Modifier.padding(18.dp),
                                lineHeight = 24.sp
                            )
                        }
                    }
                }
                "Commented Code" -> {
                    // TAB: Commented Code
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
                                "Documented Source",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = CppThemeTextPrimary
                            )
                            ModernCopyButton(textToCopy = viewModel.lastCommentedCode ?: "", label = "Copy Code")
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            color = CppThemeEditorDark,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = viewModel.lastCommentedCode ?: "// No commented code",
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
                "Optimized Code" -> {
                    // TAB: Optimized Loop
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
                                    "Iterative Rewrite",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = CppThemeTextPrimary
                                )
                                if (opt?.verified == true) {
                                    Surface(
                                        color = CppThemeSuccessSubtle,
                                        shape = RoundedCornerShape(6.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, CppThemeSuccess.copy(alpha = 0.25f))
                                    ) {
                                        Text(
                                            text = if (opt.speedup > 1.0) "${opt.speedup}x Faster" else "Equivalent",
                                            color = CppThemeSuccess,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            ModernCopyButton(textToCopy = opt?.code ?: "", label = "Copy Loop")
                        }

                        if (!opt?.note.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = opt?.note ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = CppThemeTextSecondary
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            color = CppThemeEditorDark,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = opt?.code ?: "// No loop optimization available",
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
    }
}

/**
 * Modern, low-profile Copy Button with tactile toast confirmation.
 */
@Composable
private fun ModernCopyButton(textToCopy: String, label: String) {
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
