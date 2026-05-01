package com.secure.codereviewer.data.api

import com.google.gson.annotations.SerializedName

// Auth Request Models
data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    @SerializedName("confirmPassword")
    val confirmPassword: String
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class LogoutRequest(
    val token: String
)

// Auth Response Models
data class AuthResponse(
    val success: Boolean? = null,
    val message: String,
    val data: AuthData? = null,
    val token: String? = null,
    val user: AuthUser? = null
) {
    fun authData(): AuthData? {
        return data ?: if (!token.isNullOrBlank() && user != null) {
            AuthData(token = token, user = user)
        } else {
            null
        }
    }

    fun isSuccessfulAuth(): Boolean {
        return success != false && authData() != null
    }
}

data class AuthData(
    val token: String,
    val user: AuthUser
)

data class AuthUser(
    val id: Int,
    val name: String,
    val email: String,
    @SerializedName("created_at")
    val createdAt: String? = null
)

data class CurrentUserResponse(
    val success: Boolean? = null,
    val message: String? = null,
    val data: AuthUser? = null,
    val user: AuthUser? = null
) {
    fun authUser(): AuthUser? = data ?: user

    fun isSuccessfulUser(): Boolean {
        return success != false && authUser() != null
    }
}

data class TokenResponse(
    val token: String,
    val expiresAt: String
)

data class ApiErrorResponse(
    val success: Boolean? = null,
    val message: String? = null,
    val detail: String? = null
)
