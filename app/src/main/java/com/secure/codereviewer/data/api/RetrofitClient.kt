package com.secure.codereviewer.data.api

import com.secure.codereviewer.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private val BASE_URL = BuildConfig.API_BASE_URL
    private const val CONNECT_TIMEOUT_SECONDS = 30L
    private const val MODEL_TIMEOUT_SECONDS = 300L

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
        redactHeader("Authorization")
    }

    private val authInterceptor = object : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val originalRequest = chain.request()
            val authHeader = AuthManager.getAuthHeader()
            
            return if (authHeader != null) {
                val authenticatedRequest = originalRequest.newBuilder()
                    .header("Authorization", authHeader)
                    .build()
                chain.proceed(authenticatedRequest)
            } else {
                chain.proceed(originalRequest)
            }
        }
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(logging)
        .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(MODEL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(MODEL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .callTimeout(MODEL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    val api: CodeApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CodeApiService::class.java)
    }
}
