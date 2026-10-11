package com.secure.codereviewer.data.repository

import com.secure.codereviewer.data.api.AnalyzeRequest
import com.secure.codereviewer.data.api.AnalyzeResponse
import com.secure.codereviewer.data.api.CodeValidationRequest
import com.secure.codereviewer.data.api.CodeValidationResponse
import com.secure.codereviewer.data.api.HistoryListResponse
import com.secure.codereviewer.data.api.OptimizeRequest
import com.secure.codereviewer.data.api.OptimizeResponse
import com.secure.codereviewer.data.api.RetrofitClient
import com.secure.codereviewer.data.local.HistoryStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.SocketTimeoutException

/**
 * Repository layer that coordinates network calls and caching.
 * All functions use Dispatchers.IO so network operations do not block the UI.
 */
class CodeRepository {

    /**
     * Send C++ code for full AI analysis.
     * @param code The C++ source code to analyze.
     * @param outputLanguage "english" or "roman_urdu".
     * @return AnalyzeResponse containing explanation and commented code.
     */
    suspend fun analyzeCode(
        code: String,
        outputLanguage: String = "english"
    ): AnalyzeResponse = withContext(Dispatchers.IO) {
        try {
            val response = RetrofitClient.api.analyzeCode(
                AnalyzeRequest(
                    code = code,
                    output_language = outputLanguage,
                    source = "android"
                )
            )
            if (response.isSuccessful) {
                response.body() ?: throw Exception("Empty response from server")
            } else {
                throw Exception(response.message().ifBlank { "Failed to analyze code" })
            }
        } catch (e: SocketTimeoutException) {
            throw Exception("Analysis timed out. The backend model took too long to respond.", e)
        }
    }

    /**
     * Check C++ code syntax and get auto-fix suggestions if available.
     * Fast response using tree-sitter parser without invoking the heavy AI model.
     * @param code The C++ source code to validate.
     * @return CodeValidationResponse with validation status and optional SuggestedFix.
     */
    suspend fun validateCode(code: String): CodeValidationResponse = withContext(Dispatchers.IO) {
        try {
            val response = RetrofitClient.api.validateCode(CodeValidationRequest(code = code))
            if (response.isSuccessful) {
                response.body() ?: throw Exception("Empty response from validation server")
            } else {
                throw Exception(response.message().ifBlank { "Failed to validate code" })
            }
        } catch (e: Exception) {
            throw Exception(e.message ?: "Failed to validate code", e)
        }
    }

    /**
     * Request optimization (converts recursion to iterative loop).
     * @param code The recursive C++ code to optimize.
     * @return OptimizeResponse containing the rewritten loop and speedup verification.
     */
    suspend fun optimizeCode(code: String): OptimizeResponse = withContext(Dispatchers.IO) {
        try {
            val response = RetrofitClient.api.optimizeCode(
                OptimizeRequest(
                    code = code,
                    source = "android",
                    mode = "loop"
                )
            )
            if (response.isSuccessful) {
                response.body() ?: throw Exception("Empty response from optimizer")
            } else {
                throw Exception(response.message().ifBlank { "Failed to optimize code" })
            }
        } catch (e: SocketTimeoutException) {
            throw Exception("Optimization timed out. The backend took too long to respond.", e)
        }
    }

    /**
     * Fetch paginated history from the server.
     */
    suspend fun getHistory(limit: Int, offset: Int): HistoryListResponse = withContext(Dispatchers.IO) {
        try {
            val response = RetrofitClient.api.getHistory(limit = limit, offset = offset)
            if (response.isSuccessful) {
                val body = response.body() ?: throw Exception("Empty response from server")
                if (offset == 0) {
                    HistoryStorage.saveHistoryCache(body.items, body.total)
                }
                body
            } else {
                val message = when (response.code()) {
                    401 -> "Session expired. Please log in again."
                    else -> response.message().ifBlank { "Failed to load history" }
                }
                throw Exception(message)
            }
        } catch (e: SocketTimeoutException) {
            throw Exception("History request timed out. Please try again.", e)
        }
    }

    /**
     * Get offline cached history (file read + JSON parse on Dispatchers.IO).
     */
    suspend fun getLocalHistory(): HistoryListResponse? {
        val cached = HistoryStorage.getHistoryCache() ?: return null
        return HistoryListResponse(
            items = cached.first,
            total = cached.second,
            limit = 20,
            offset = 0
        )
    }
}
