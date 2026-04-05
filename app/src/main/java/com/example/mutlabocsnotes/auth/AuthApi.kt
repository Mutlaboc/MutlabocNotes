package com.example.mutlabocsnotes.auth

import retrofit2.http.Body
import retrofit2.http.POST

// Retrofit contract with backend endpoints for this feature.
interface AuthApi {
    // Authenticates the user and updates local auth state.
    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequestDto
    ): AuthResponseDto

    // Registers a user and updates local auth state.
    @POST("auth/register")
    suspend fun register(
        @Body request: RegisterRequestDto
    ): AuthResponseDto

    // Exchanges a Google token for an authenticated backend session.
    @POST("auth/social/google")
    suspend fun loginWithGoogle(
        @Body request: GoogleSocialLoginRequestDto
    ): AuthResponseDto

    // Exchanges a Yandex token for an authenticated backend session.
    @POST("auth/social/yandex")
    suspend fun loginWithYandex(
        @Body request: YandexSocialLoginRequestDto
    ): AuthResponseDto
}
