package com.example.mutlabocsnotes.auth

import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("auth/social/google")
    suspend fun loginWithGoogle(
        @Body request: GoogleSocialLoginRequestDto
    ): AuthResponseDto
}
