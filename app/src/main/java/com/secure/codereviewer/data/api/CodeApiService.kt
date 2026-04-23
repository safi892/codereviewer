package com.secure.codereviewer.data.api

import retrofit2.http.Body
import retrofit2.http.POST

interface CodeApiService {
    @POST("analyze")
    suspend fun analyzeCode(
        @Body request: AnalyzeRequest
    ): AnalyzeResponse
}
