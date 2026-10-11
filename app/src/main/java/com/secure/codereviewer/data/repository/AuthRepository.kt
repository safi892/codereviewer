package com.secure.codereviewer.data.repository

import com.google.gson.Gson
import com.secure.codereviewer.data.api.*
import com.secure.codereviewer.data.local.AuthStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

class AuthRepository {
    private val gson = Gson()

    suspend fun register(
        name: String,
        email: String,
        password: String,
        confirmPassword: String
    ): Result<AuthData> = withContext(Dispatchers.IO) {
        try {
            val request = RegisterRequest(
                name = name,
                email = email,
                password = password,
                confirmPassword = confirmPassword
            )
            val response = RetrofitClient.api.register(request)
            val body = response.body()
            val authData = body?.authData()
            if (response.isSuccessful && body?.isSuccessfulAuth() == true && authData != null) {
                AuthStorage.saveAuthData(authData.token, authData.user)
                Result.success(authData)
            } else {
                Result.failure(Exception(response.errorMessage(body?.message)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun login(email: String, password: String): Result<AuthData> = withContext(Dispatchers.IO) {
        try {
            val request = LoginRequest(email = email, password = password)
            val response = RetrofitClient.api.login(request)
            val body = response.body()
            val authData = body?.authData()
            if (response.isSuccessful && body?.isSuccessfulAuth() == true && authData != null) {
                AuthStorage.saveAuthData(authData.token, authData.user)
                Result.success(authData)
            } else {
                Result.failure(Exception(response.errorMessage(body?.message)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCurrentUser(): Result<AuthUser> = withContext(Dispatchers.IO) {
        try {
            if (AuthStorage.getAuthHeader() == null) {
                return@withContext Result.failure(Exception("No token found"))
            }

            val response = RetrofitClient.api.getCurrentUser()
            val body = response.body()
            val user = body?.authUser()
            if (response.isSuccessful && body?.isSuccessfulUser() == true && user != null) {
                Result.success(user)
            } else {
                Result.failure(Exception(response.errorMessage(body?.message)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logout(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (AuthStorage.getAuthHeader() != null) {
                RetrofitClient.api.logout()
            }
            AuthStorage.logout()
            Result.success(Unit)
        } catch (e: Exception) {
            AuthStorage.logout()
            Result.failure(e)
        }
    }

    fun isLoggedIn(): Boolean = AuthStorage.isLoggedIn()

    fun getCurrentUserInfo(): AuthUser? {
        val id = AuthStorage.getUserId()
        val name = AuthStorage.getUserName()
        val email = AuthStorage.getUserEmail()
        return if (id > 0 && name != null && email != null) {
            AuthUser(id = id, name = name, email = email)
        } else {
            null
        }
    }

    private fun <T> Response<T>.errorMessage(fallback: String?): String {
        if (!fallback.isNullOrBlank()) return fallback

        val parsedError = try {
            errorBody()?.string()?.takeIf { it.isNotBlank() }?.let {
                gson.fromJson(it, ApiErrorResponse::class.java)
            }
        } catch (_: Exception) {
            null
        }

        return parsedError?.message
            ?: parsedError?.detail
            ?: message().takeIf { it.isNotBlank() }
            ?: "Request failed. Please try again."
    }
}
