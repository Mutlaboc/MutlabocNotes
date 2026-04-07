package com.example.mutlabocsnotes.auth

import retrofit2.http.Body
import retrofit2.http.POST

// Retrofit-контракт с backend-эндпоинтами этой функциональности.
interface AuthApi {
    // Выполняет аутентификацию пользователя и обновляет локальное состояние авторизации.
    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequestDto
    ): AuthResponseDto

    // Регистрирует пользователя и обновляет локальное состояние авторизации.
    @POST("auth/register")
    suspend fun register(
        @Body request: RegisterRequestDto
    ): AuthResponseDto

    // Обменивает Google-токен на аутентифицированную backend-сессию.
    @POST("auth/social/google")
    suspend fun loginWithGoogle(
        @Body request: GoogleSocialLoginRequestDto
    ): AuthResponseDto

    // Обменивает Yandex-токен на аутентифицированную backend-сессию.
    @POST("auth/social/yandex")
    suspend fun loginWithYandex(
        @Body request: YandexSocialLoginRequestDto
    ): AuthResponseDto
}
