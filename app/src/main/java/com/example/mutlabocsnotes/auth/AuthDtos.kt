package com.example.mutlabocsnotes.auth

// Модель данных для request payload, отправляемого в backend.
data class LoginRequestDto(
    val email: String,
    val password: String
)

// Модель данных для request payload, отправляемого в backend.
data class RegisterRequestDto(
    val email: String,
    val password: String,
    val displayName: String? = null
)

// Модель данных для request payload, отправляемого в backend.
data class GoogleSocialLoginRequestDto(
    val idToken: String
)

// Модель данных для request payload, отправляемого в backend.
data class YandexSocialLoginRequestDto(
    val accessToken: String
)

// Модель данных для ответов, возвращаемых backend.
data class AuthUserResponseDto(
    val id: String,
    val email: String,
    val displayName: String? = null,
    val bridgeUserKey: String? = null
)

// Модель данных для ответов, возвращаемых backend.
data class AuthResponseDto(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer",
    val expiresInSeconds: Long,
    val refreshExpiresInSeconds: Long,
    val bridgeUserKey: String? = null,
    val user: AuthUserResponseDto
)
