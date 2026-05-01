package com.example.mutlabocsnotes.auth

import android.content.Context
import com.example.mutlabocsnotes.BuildConfig
import com.example.mutlabocsnotes.SessionManager as AppSessionManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

// Инкапсулирует доступ к данным и бизнес-операции.
class BackendAuthRepository(context: Context) {

    private val sessionManager: AppSessionManager = AppSessionManager(context)

    private val authApi: AuthApi by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
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

    // Сохраняет пару токенов и определённый email в локальном хранилище сессии.
    private fun persistSession(response: AuthResponseDto) {
        sessionManager.saveSession(
            accessToken = response.accessToken,
            refreshToken = response.refreshToken,
            email = response.user.email
        )
    }

    // Выполняет аутентификацию пользователя и обновляет локальное состояние авторизации.
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

    // Регистрирует пользователя и обновляет локальное состояние авторизации.
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

    // Выполняет аутентификацию пользователя и обновляет локальное состояние авторизации.
    suspend fun signInWithGoogle(idToken: String): AuthResponseDto {
        val response = authApi.loginWithGoogle(
            GoogleSocialLoginRequestDto(idToken = idToken)
        )
        persistSession(response)
        return response
    }

    // Выполняет аутентификацию пользователя и обновляет локальное состояние авторизации.
    suspend fun signInWithYandex(accessToken: String): AuthResponseDto {
        val response = authApi.loginWithYandex(
            YandexSocialLoginRequestDto(accessToken = accessToken)
        )
        persistSession(response)
        return response
    }

    // Возвращает, выполнено ли требуемое условие в текущий момент.
    fun isLoggedIn(): Boolean = sessionManager.hasSession()

    // Возвращает данные из текущего источника.
    fun getUserEmail(): String = sessionManager.getEmail().orEmpty()

    // Завершает текущую сессию и очищает данные авторизации.
    fun logout() {
        sessionManager.clear()
    }
}
