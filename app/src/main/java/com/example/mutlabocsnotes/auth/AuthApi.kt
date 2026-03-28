package com.example.mutlabocsnotes.auth

import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequestDto
    ): AuthResponseDto

    @POST("auth/register")
    suspend fun register(
        @Body request: RegisterRequestDto
    ): AuthResponseDto

    @POST("auth/social/google")
    suspend fun loginWithGoogle(
        @Body request: GoogleSocialLoginRequestDto
    ): AuthResponseDto

    @POST("auth/social/yandex")
    suspend fun loginWithYandex(
        @Body request: YandexSocialLoginRequestDto
    ): AuthResponseDto
}
