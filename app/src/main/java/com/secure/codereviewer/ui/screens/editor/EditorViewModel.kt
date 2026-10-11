package com.secure.codereviewer.ui.screens.editor

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.secure.codereviewer.data.api.AnalyzeResponse
import com.secure.codereviewer.data.api.CodeValidationResponse
import com.secure.codereviewer.data.api.OptimizeResponse
import com.secure.codereviewer.data.api.SuggestedFix
import com.secure.codereviewer.data.repository.CodeRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "EditorViewModel"
private const val VALIDATION_DEBOUNCE_MS = 500L

/**
 * ViewModel for EditorScreen managing code content, validation,
 * optimization, and analysis state.
 */
@OptIn(FlowPreview::class)
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
     * Bumped only when the ViewModel replaces the text (open file, auto-fix, new file...).
     * The editor screen observes this to push text into the CodeEditor. Typing does NOT
     * bump it, so we never copy the whole document back into the view per keystroke.
     */
    var externalContentVersion by mutableIntStateOf(0)
        private set

    /** Raw text stream; validation is derived from it with debounce + collectLatest. */
    private val validationInput = MutableStateFlow("")

    init {
        viewModelScope.launch {
            validationInput
                .debounce(VALIDATION_DEBOUNCE_MS)   // wait for the user to pause typing
                .distinctUntilChanged()             // skip if text didn't really change
                .collectLatest { text -> validate(text) } // cancels in-flight request on new text
        }
    }

    /** Called by the editor (already debounced in the UI) with a fresh text snapshot. */
    fun onEditorTextChanged(text: String) {
        currentContent = text
        validationInput.value = text
    }

    /** Make sure we work with the latest editor text before analyze/save. */
    fun syncFromEditor(text: String?) {
        if (text != null && text != currentContent) onEditorTextChanged(text)
    }

    private fun setContentFromViewModel(text: String) {
        currentContent = text
        externalContentVersion++
        validationInput.value = text
    }

    private suspend fun validate(text: String) {
        if (text.isBlank()) {
            isSyntaxValid = true
            validationMessage = null
            validationErrorLine = null
            currentSuggestedFix = null
            return
        }
        isValidating = true
        try {
            val response: CodeValidationResponse = repository.validateCode(text)
            isSyntaxValid = response.valid
            validationMessage = response.message
            validationErrorLine = response.line
            currentSuggestedFix = response.suggested_fix
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            validationMessage = null
            currentSuggestedFix = null
        } finally {
            isValidating = false
        }
    }

    /**
     * Apply the suggested auto-fix returned by tree-sitter.
     */
    fun applySuggestedFix() {
        val fix = currentSuggestedFix ?: return
        currentSuggestedFix = null
        validationMessage = "Auto-fix applied"
        isSyntaxValid = true
        setContentFromViewModel(fix.code)
    }

    /**
     * Open external or transferred code into the editor.
     */
    fun openContent(content: String, fileName: String = "main.cpp") {
        currentFileName = fileName
        currentUri = null
        analysisError = null
        lastExplanation = null
        lastCommentedCode = null
        optimizationResult = null
        setContentFromViewModel(content)
    }

    /**
     * Load file from device storage via Uri. Disk/provider I/O runs on Dispatchers.IO.
     * Takes a ContentResolver (not an Activity) so the ViewModel never holds a UI reference.
     */
    fun loadFile(uri: Uri, resolver: ContentResolver) {
        viewModelScope.launch {
            val content = withContext(Dispatchers.IO) {
                runCatching {
                    resolver.openInputStream(uri)?.use { it.bufferedReader().readText() }
                }.onFailure { Log.e(TAG, "Failed to read $uri", it) }.getOrNull()
            } ?: return@launch
            currentUri = uri
            currentFileName = uri.lastPathSegment?.substringAfterLast('/') ?: "main.cpp"
            setContentFromViewModel(content)
        }
    }

    /**
     * Save code to the given Uri on Dispatchers.IO; [onResult] is called on the main thread.
     */
    fun saveToUri(uri: Uri, resolver: ContentResolver, onResult: (Boolean) -> Unit = {}) {
        val snapshot = currentContent
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    resolver.openOutputStream(uri, "wt")?.use { it.write(snapshot.toByteArray()) } != null
                }.onFailure { Log.e(TAG, "Failed to save $uri", it) }.getOrDefault(false)
            }
            if (ok) {
                currentUri = uri
                currentFileName = uri.lastPathSegment?.substringAfterLast('/') ?: "main.cpp"
            }
            onResult(ok)
        }
    }

    fun saveCurrentFile(resolver: ContentResolver, onResult: (Boolean) -> Unit = {}) {
        val uri = currentUri ?: return onResult(false)
        saveToUri(uri, resolver, onResult)
    }

    fun newFile() {
        currentFileName = "main.cpp"
        currentUri = null
        analysisError = null
        lastExplanation = null
        lastCommentedCode = null
        optimizationResult = null
        currentSuggestedFix = null
        validationMessage = null
        setContentFromViewModel("")
    }

    /**
     * Run full code review / analysis pipeline:
     * Calls POST /analyze (English standard).
     * If optimization is toggled, also requests POST /optimize.
     */
    fun analyzeCurrentCode(onAnalysisComplete: (AnalyzeResponse, OptimizeResponse?) -> Unit = { _, _ -> }) {
        if (isAnalyzing || currentContent.isBlank()) return
        isAnalyzing = true
        analysisError = null
        lastExplanation = null
        lastCommentedCode = null
        optimizationResult = null

        val code = currentContent
        val runOptimization = isOptimizationEnabled

        viewModelScope.launch {
            try {
                // /analyze and /optimize are independent: run them concurrently.
                // coroutineScope = structured concurrency: if analysis fails, the
                // optimization request is cancelled automatically.
                val (result, opt) = coroutineScope {
                    val optimization = if (runOptimization) {
                        isOptimizing = true
                        async {
                            try {
                                repository.optimizeCode(code)
                            } catch (e: CancellationException) {
                                throw e
                            } catch (_: Exception) {
                                null // supplementary; keep analysis even if this fails
                            } finally {
                                isOptimizing = false
                            }
                        }
                    } else null

                    val analysis = repository.analyzeCode(code = code, outputLanguage = "english")
                    analysis to optimization?.await()
                }

                lastExplanation = result.explanation
                lastCommentedCode = result.commented_code
                needsReview = result.needs_review
                optimizationResult = opt

                selectedResultTab = 0
                onAnalysisComplete(result, opt)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                analysisError = e.message ?: "Failed to analyze code"
            } finally {
                isOptimizing = false
                isAnalyzing = false
            }
        }
    }

    /**
     * Run loop optimization only (without running /analyze):
     * Directly calls POST /optimize to convert recursion to loop,
     * showing only the optimized rewrite and verification badges.
     */
    fun optimizeCurrentCode(onOptimizationComplete: (OptimizeResponse) -> Unit = {}) {
        if (isOptimizing || currentContent.isBlank()) return
        isOptimizing = true
        analysisError = null
        optimizationResult = null

        viewModelScope.launch {
            try {
                val opt = repository.optimizeCode(currentContent)
                optimizationResult = opt
                if (opt.explanation.isNotBlank()) {
                    lastExplanation = opt.explanation
                }
                lastCommentedCode = opt.code
                selectedResultTab = 0
                onOptimizationComplete(opt)
            } catch (e: Exception) {
                analysisError = e.message ?: "Failed to optimize code"
            } finally {
                isOptimizing = false
            }
        }
    }
}
