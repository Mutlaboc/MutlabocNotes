package com.example.mutlabocsnotes.auth

data class GoogleSocialLoginRequestDto(
    val idToken: String
)

data class AuthUserResponseDto(
    val id: String,
    val email: String,
    val displayName: String? = null,
    val bridgeUserKey: String? = null
)

data class AuthResponseDto(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer",
    val expiresInSeconds: Long,
    val refreshExpiresInSeconds: Long,
    val bridgeUserKey: String? = null,
    val user: AuthUserResponseDto
)
