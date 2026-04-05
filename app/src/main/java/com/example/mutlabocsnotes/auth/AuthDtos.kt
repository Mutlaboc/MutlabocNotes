package com.example.mutlabocsnotes.auth

// Data model for request payloads sent to the backend.
data class LoginRequestDto(
    val email: String,
    val password: String
)

// Data model for request payloads sent to the backend.
data class RegisterRequestDto(
    val email: String,
    val password: String,
    val displayName: String? = null
)

// Data model for request payloads sent to the backend.
data class GoogleSocialLoginRequestDto(
    val idToken: String
)

// Data model for request payloads sent to the backend.
data class YandexSocialLoginRequestDto(
    val accessToken: String
)

// Data model for responses returned by the backend.
data class AuthUserResponseDto(
    val id: String,
    val email: String,
    val displayName: String? = null,
    val bridgeUserKey: String? = null
)

// Data model for responses returned by the backend.
data class AuthResponseDto(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer",
    val expiresInSeconds: Long,
    val refreshExpiresInSeconds: Long,
    val bridgeUserKey: String? = null,
    val user: AuthUserResponseDto
)
