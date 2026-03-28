package com.example.mutlabocsnotes.auth

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class BackendAuthRepository(context: Context) {

    private val sessionManager = SessionManager(context)

    private val authApi: AuthApi by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        Retrofit.Builder()
            .baseUrl(ApiConfig.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AuthApi::class.java)
    }

    suspend fun signInWithGoogle(idToken: String): AuthResponseDto {
        val response = authApi.loginWithGoogle(
            GoogleSocialLoginRequestDto(idToken = idToken)
        )
        sessionManager.saveSession(response)
        return response
    }

    fun isLoggedIn(): Boolean = sessionManager.isLoggedIn()

    fun getUserEmail(): String = sessionManager.getUserEmail()

    fun logout() {
        sessionManager.clearSession()
    }
}
