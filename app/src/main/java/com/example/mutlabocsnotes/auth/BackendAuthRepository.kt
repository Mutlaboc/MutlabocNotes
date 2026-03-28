package com.example.mutlabocsnotes.auth

import android.content.Context
import com.example.mutlabocsnotes.SessionManager as AppSessionManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class BackendAuthRepository(context: Context) {

    private val sessionManager: AppSessionManager = AppSessionManager(context)

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

    private fun persistSession(response: AuthResponseDto) {
        sessionManager.saveSession(
            accessToken = response.accessToken,
            refreshToken = response.refreshToken,
            email = response.user.email
        )
    }

    suspend fun signInWithEmail(email: String, password: String): AuthResponseDto {
        val response = authApi.login(
            LoginRequestDto(
                email = email,
                password = password
            )
        )
        persistSession(response)
        return response
    }

    suspend fun signUpWithEmail(
        email: String,
        password: String,
        displayName: String? = null
    ): AuthResponseDto {
        val response = authApi.register(
            RegisterRequestDto(
                email = email,
                password = password,
                displayName = displayName
            )
        )
        persistSession(response)
        return response
    }

    suspend fun signInWithGoogle(idToken: String): AuthResponseDto {
        val response = authApi.loginWithGoogle(
            GoogleSocialLoginRequestDto(idToken = idToken)
        )
        persistSession(response)
        return response
    }

    fun isLoggedIn(): Boolean = sessionManager.hasSession()

    fun getUserEmail(): String = sessionManager.getEmail().orEmpty()

    fun logout() {
        sessionManager.clear()
    }
}
