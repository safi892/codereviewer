package com.secure.codereviewer.data.repository

import com.secure.codereviewer.data.api.AnalyzeRequest
import com.secure.codereviewer.data.api.AnalyzeResponse
import com.secure.codereviewer.data.api.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CodeRepository {
    suspend fun analyzeCode(code: String): AnalyzeResponse = withContext(Dispatchers.IO) {
        RetrofitClient.api.analyzeCode(AnalyzeRequest(code = code))
    }
}
