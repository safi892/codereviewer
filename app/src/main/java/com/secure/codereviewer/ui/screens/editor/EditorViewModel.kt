package com.secure.codereviewer.ui.screens.editor

import android.app.Activity
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.secure.codereviewer.data.api.AnalyzeResponse
import com.secure.codereviewer.data.repository.CodeRepository
import kotlinx.coroutines.launch

class EditorViewModel : ViewModel() {
    private val repository = CodeRepository()

    var currentContent by mutableStateOf("")
    var currentFileName by mutableStateOf("untitled.cpp")
    var currentUri by mutableStateOf<Uri?>(null)
    var isAnalyzing by mutableStateOf(false)
    var analysisError by mutableStateOf<String?>(null)
    var lastExplanation by mutableStateOf<String?>(null)

    fun updateContent(text: String) {
        currentContent = text
    }

    fun openContent(content: String, fileName: String = "untitled.cpp") {
        currentContent = content
        currentFileName = fileName
        currentUri = null
        analysisError = null
        lastExplanation = null
    }

    fun loadFile(uri: Uri, activity: Activity) {
        try {
            activity.contentResolver.openInputStream(uri)?.use { input ->
                val content = input.bufferedReader().readText()
                updateContent(content)
                currentUri = uri
                currentFileName = uri.lastPathSegment?.substringAfterLast('/') ?: "loaded.cpp"
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun saveToUri(uri: Uri, activity: Activity): Boolean {
        return try {
            activity.contentResolver.openOutputStream(uri)?.use { output ->
                output.write(currentContent.toByteArray())
                currentUri = uri
                currentFileName = uri.lastPathSegment?.substringAfterLast('/') ?: "saved.cpp"
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun saveCurrentFile(activity: Activity): Boolean {
        return currentUri?.let { saveToUri(it, activity) } ?: false
    }

    fun newFile() {
        currentContent = ""
        currentFileName = "untitled.cpp"
        currentUri = null
    }

    fun analyzeCurrentCode(onAnalysisComplete: (AnalyzeResponse) -> Unit = {}) {
        if (isAnalyzing) return
        isAnalyzing = true
        analysisError = null
        lastExplanation = null

        viewModelScope.launch {
            try {
                val result = repository.analyzeCode(currentContent)
                currentContent = buildCommentedOutput(result.explanation, result.commented_code)
                lastExplanation = result.explanation
                onAnalysisComplete(result)
            } catch (e: Exception) {
                analysisError = e.message ?: "Failed to analyze code"
            } finally {
                isAnalyzing = false
            }
        }
    }

    private fun buildCommentedOutput(explanation: String, commentedCode: String): String {
        val commentedExplanation = explanation
            .trim()
            .lines()
            .filter { it.isNotBlank() }
            .joinToString("\n") { line -> "// ${line.trim()}" }
        val cleanedCode = commentedCode.trim()
        val codeLines = cleanedCode.lines()
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
            codeLines.drop(firstFunctionIndex).joinToString("\n").trimStart()
        } else {
            cleanedCode
        }

        return listOf(header, commentedExplanation, body)
            .filter { it.isNotBlank() }
            .joinToString("\n\n")
    }
}
