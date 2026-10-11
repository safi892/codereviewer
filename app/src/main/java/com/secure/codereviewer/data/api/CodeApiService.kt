package com.secure.codereviewer.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Retrofit service interface defining the backend endpoints.
 * Everything is suspended so coroutines run these off the main thread.
 */
interface CodeApiService {
    // --- Auth Endpoints ---
    @POST("auth/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<AuthResponse>

    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<AuthResponse>

    @GET("auth/me")
    suspend fun getCurrentUser(): Response<CurrentUserResponse>

    @POST("auth/logout")
    suspend fun logout(): Response<AuthResponse>

    // --- Code Analysis Endpoints ---

    // 1. Full AI analysis (explanation + commented code)
    @POST("analyze")
    suspend fun analyzeCode(
        @Body request: AnalyzeRequest
    ): Response<AnalyzeResponse>

    // 2. Syntax validation & auto-fix proposal (fast, tree-sitter based)
    @POST("validate-code")
    suspend fun validateCode(
        @Body request: CodeValidationRequest
    ): Response<CodeValidationResponse>

    // 3. Optimization endpoint (converts recursion to loop and verifies equivalence)
    @POST("optimize")
    suspend fun optimizeCode(
        @Body request: OptimizeRequest
    ): Response<OptimizeResponse>

    // 4. Analysis history
    @GET("analyze/history")
    suspend fun getHistory(
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0
    ): Response<HistoryListResponse>
}
