package com.example.mutlabocsnotes.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

// Retrofit-контракт с backend-эндпоинтами этой функциональности.
interface AuthApi {

    // Сетевой контракт на регистрацию пользователя.
    @POST("auth/register")
    suspend fun register(
        @Body request: AuthCredentialsDto
    ): AuthResponseDto

    // Сетевой контракт на аутентификацию пользователя.
    @POST("auth/login")
    suspend fun login(
        @Body request: AuthCredentialsDto
    ): AuthResponseDto

    // Запрашивает новую пару access/refresh токенов по refresh token.
    @POST("auth/refresh")
    suspend fun refresh(
        @Body request: RefreshTokenRequestDto
    ): AuthResponseDto

    // Запрашивает профиль текущего аутентифицированного пользователя.
    @GET("auth/me")
    suspend fun me(
        @Header("Authorization") authorization: String
    ): MeResponseDto
}
