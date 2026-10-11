package com.secure.codereviewer.ui.screens.editor

import android.app.Activity
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.secure.codereviewer.data.api.AnalyzeResponse
import com.secure.codereviewer.data.api.CodeValidationResponse
import com.secure.codereviewer.data.api.OptimizeResponse
import com.secure.codereviewer.data.api.SuggestedFix
import com.secure.codereviewer.data.repository.CodeRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * ViewModel for EditorScreen managing code content, validation,
 * optimization, and analysis state.
 */
class EditorViewModel : ViewModel() {
    private val repository = CodeRepository()

    // --- Core Editor State ---
    var currentContent by mutableStateOf("")
    var currentFileName by mutableStateOf("main.cpp")
    var currentUri by mutableStateOf<Uri?>(null)

    // --- Optimization Toggle (Convert recursion to loop) ---
    var isOptimizationEnabled by mutableStateOf(false)
    var isOptimizing by mutableStateOf(false)
    var optimizationResult by mutableStateOf<OptimizeResponse?>(null)

    // --- Syntax Validation & Quick Fix State ---
    var isValidating by mutableStateOf(false)
    var isSyntaxValid by mutableStateOf(true)
    var validationMessage by mutableStateOf<String?>(null)
    var validationErrorLine by mutableStateOf<Int?>(null)
    var currentSuggestedFix by mutableStateOf<SuggestedFix?>(null)
    private var validationJob: Job? = null

    // --- Analysis State ---
    var isAnalyzing by mutableStateOf(false)
    var analysisError by mutableStateOf<String?>(null)
    var lastExplanation by mutableStateOf<String?>(null)
    var lastCommentedCode by mutableStateOf<String?>(null)
    var needsReview by mutableStateOf(false)

    // --- Results View State (Tabs: 0 = Explanation, 1 = Commented Code, 2 = Optimized Code) ---
    var selectedResultTab by mutableStateOf(0)
    var showResultsSheet by mutableStateOf(false)

    /**
     * Called whenever text changes in the editor.
     * Triggers debounced validation so we don't spam the server on every keystroke.
     */
    fun updateContent(text: String) {
        currentContent = text
        scheduleValidation(text)
    }

    /**
     * Debounced syntax validation. Waits 500ms after user stops typing.
     */
    fun scheduleValidation(text: String) {
        validationJob?.cancel()
        if (text.isBlank()) {
            isSyntaxValid = true
            validationMessage = null
            validationErrorLine = null
            currentSuggestedFix = null
            return
        }

        validationJob = viewModelScope.launch {
            delay(500) // Wait for user to pause typing
            isValidating = true
            try {
                val response: CodeValidationResponse = repository.validateCode(text)
                isSyntaxValid = response.valid
                validationMessage = response.message
                validationErrorLine = response.line
                currentSuggestedFix = response.suggested_fix
            } catch (_: Exception) {
                validationMessage = null
                currentSuggestedFix = null
            } finally {
                isValidating = false
            }
        }
    }

    /**
     * Apply the suggested auto-fix returned by tree-sitter.
     */
    fun applySuggestedFix() {
        val fix = currentSuggestedFix ?: return
        currentContent = fix.code
        currentSuggestedFix = null
        validationMessage = "Auto-fix applied"
        isSyntaxValid = true
        scheduleValidation(fix.code)
    }

    /**
     * Open external or transferred code into the editor.
     */
    fun openContent(content: String, fileName: String = "main.cpp") {
        currentContent = content
        currentFileName = fileName
        currentUri = null
        analysisError = null
        lastExplanation = null
        lastCommentedCode = null
        optimizationResult = null
        scheduleValidation(content)
    }

    /**
     * Load file from device storage via Uri.
     */
    fun loadFile(uri: Uri, activity: Activity) {
        try {
            activity.contentResolver.openInputStream(uri)?.use { input ->
                val content = input.bufferedReader().readText()
                updateContent(content)
                currentUri = uri
                currentFileName = uri.lastPathSegment?.substringAfterLast('/') ?: "main.cpp"
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Save code to specified device Uri.
     */
    fun saveToUri(uri: Uri, activity: Activity): Boolean {
        return try {
            activity.contentResolver.openOutputStream(uri)?.use { output ->
                output.write(currentContent.toByteArray())
                currentUri = uri
                currentFileName = uri.lastPathSegment?.substringAfterLast('/') ?: "main.cpp"
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
        currentFileName = "main.cpp"
        currentUri = null
        analysisError = null
        lastExplanation = null
        lastCommentedCode = null
        optimizationResult = null
        currentSuggestedFix = null
        validationMessage = null
    }

    /**
     * Run full code review / analysis pipeline:
     * Calls POST /analyze (English standard).
     * If optimization is toggled, also requests POST /optimize.
     */
    fun analyzeCurrentCode(onAnalysisComplete: (AnalyzeResponse) -> Unit = {}) {
        if (isAnalyzing || currentContent.isBlank()) return
        isAnalyzing = true
        analysisError = null
        lastExplanation = null
        lastCommentedCode = null
        optimizationResult = null

        viewModelScope.launch {
            try {
                // Step 1: Request analysis & explanation
                val result = repository.analyzeCode(
                    code = currentContent,
                    outputLanguage = "english"
                )
                lastExplanation = result.explanation
                lastCommentedCode = result.commented_code
                needsReview = result.needs_review

                // Step 2: If recursion-to-loop optimization requested, run it
                if (isOptimizationEnabled) {
                    try {
                        isOptimizing = true
                        val opt = repository.optimizeCode(currentContent)
                        optimizationResult = opt
                    } catch (_: Exception) {
                        // Optimization is supplementary; keep analysis result even if optimization fails
                    } finally {
                        isOptimizing = false
                    }
                }

                // Step 3: Open results panel
                selectedResultTab = 0
                showResultsSheet = true
                onAnalysisComplete(result)
            } catch (e: Exception) {
                analysisError = e.message ?: "Failed to analyze code"
            } finally {
                isAnalyzing = false
            }
        }
    }

    /**
     * Run loop optimization only (without running /analyze):
     * Directly calls POST /optimize to convert recursion to loop,
     * showing only the optimized rewrite and verification badges.
     */
    fun optimizeCurrentCode() {
        if (isOptimizing || currentContent.isBlank()) return
        isOptimizing = true
        analysisError = null
        optimizationResult = null

        viewModelScope.launch {
            try {
                val opt = repository.optimizeCode(currentContent)
                optimizationResult = opt
                selectedResultTab = 2
                showResultsSheet = true
            } catch (e: Exception) {
                analysisError = e.message ?: "Failed to optimize code"
            } finally {
                isOptimizing = false
            }
        }
    }
}
