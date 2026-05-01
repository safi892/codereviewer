package com.secure.codereviewer.data.api

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.Response

interface CodeApiService {
    // Auth Endpoints
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

    // Code Analysis Endpoints
    @POST("analyze")
    suspend fun analyzeCode(
        @Body request: AnalyzeRequest
    ): Response<AnalyzeResponse>
}
