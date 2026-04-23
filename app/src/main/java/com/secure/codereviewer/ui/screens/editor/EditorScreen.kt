package com.secure.codereviewer.ui.screens.editor

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import io.github.rosemoe.sora.widget.CodeEditor
import androidx.lifecycle.viewmodel.compose.viewModel
import com.secure.codereviewer.util.EditorSetup

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    onBack: () -> Unit,
    onFileOpened: (String) -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val editorViewModel: EditorViewModel = viewModel()
    var editorRef by remember { mutableStateOf<CodeEditor?>(null) }

    val openFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (activity != null && uri != null) {
            editorViewModel.loadFile(uri, activity)
            onFileOpened(editorViewModel.currentFileName)
        }
    }

    val saveFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/x-c++src")
    ) { uri: Uri? ->
        if (activity != null && uri != null) {
            editorViewModel.saveToUri(uri, activity)
            onFileOpened(editorViewModel.currentFileName)
        }
    }

    LaunchedEffect(editorViewModel.currentContent) {
        if (editorRef?.text?.toString() != editorViewModel.currentContent) {
            editorRef?.setText(editorViewModel.currentContent)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Column {
                        Text(
                            editorViewModel.currentFileName, 
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            if (editorViewModel.currentUri == null) "Unsaved" else "Saved",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (editorViewModel.currentUri == null) 
                                MaterialTheme.colorScheme.error 
                            else 
                                MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        editorViewModel.newFile()
                        editorRef?.setText("")
                    }) {
                        Icon(Icons.Default.NoteAdd, contentDescription = "New")
                    }
                    IconButton(onClick = {
                        openFileLauncher.launch(arrayOf("text/plain", "text/x-c++src"))
                    }) {
                        Icon(Icons.Default.FolderOpen, contentDescription = "Open")
                    }
                    IconButton(
                        onClick = {
                            if (activity != null) {
                                editorViewModel.saveCurrentFile(activity)
                                onFileOpened(editorViewModel.currentFileName)
                            }
                        },
                        enabled = editorViewModel.currentUri != null
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "Save")
                    }
                    IconButton(onClick = { saveFileLauncher.launch("main.cpp") }) {
                        Icon(Icons.Default.SaveAs, contentDescription = "Save As")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            EditorFileBar(fileName = editorViewModel.currentFileName)
            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            AndroidView(
                factory = { ctx ->
                    CodeEditor(ctx).apply {
                        EditorSetup.configure(this, ctx)
                        setText(
                            editorViewModel.currentContent.ifEmpty {
                                "// Start typing your C++ code here\n\n#include <iostream>\n\nint main() {\n    \n    return 0;\n}"
                            }
                        )
                        subscribeEvent(io.github.rosemoe.sora.event.ContentChangeEvent::class.java) { _, _ ->
                            editorViewModel.updateContent(text.toString())
                        }
                        editorRef = this
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
            )

            if (editorViewModel.lastExplanation != null || editorViewModel.analysisError != null) {
                ExplanationPanel(
                    explanation = editorViewModel.lastExplanation,
                    errorMessage = editorViewModel.analysisError
                )
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 4.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Button(
                    onClick = { editorViewModel.analyzeCurrentCode() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(54.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (editorViewModel.isAnalyzing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Analyzing...", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Re-analyze Code", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ExplanationPanel(
    explanation: String?,
    errorMessage: String?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = "Explanation",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(6.dp))
        if (errorMessage != null) {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        } else if (explanation != null) {
            Text(
                text = explanation,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EditorFileBar(fileName: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Code, 
            contentDescription = null, 
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = fileName,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.width(10.dp))
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        ) {
            Text(
                text = "C++",
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
