package com.secure.codereviewer.data.repository

import com.secure.codereviewer.data.api.AnalyzeRequest
import com.secure.codereviewer.data.api.AnalyzeResponse
import com.secure.codereviewer.data.api.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.SocketTimeoutException

class CodeRepository {
    suspend fun analyzeCode(code: String): AnalyzeResponse = withContext(Dispatchers.IO) {
        try {
            val response = RetrofitClient.api.analyzeCode(AnalyzeRequest(code = code))
            if (response.isSuccessful) {
                response.body() ?: throw Exception("Empty response from server")
            } else {
                throw Exception(response.message().ifBlank { "Failed to analyze code" })
            }
        } catch (e: SocketTimeoutException) {
            throw Exception("Analysis timed out. The backend model took too long to respond.", e)
        }
    }
}
