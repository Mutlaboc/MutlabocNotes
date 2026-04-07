package com.example.mutlabocsnotes.network

import com.google.gson.annotations.SerializedName

// Модель данных, общая для слоёв этого модуля.
data class AuthCredentialsDto(
    val email: String,
    val password: String
)

// Модель данных для request payload, отправляемого в backend.
data class RefreshTokenRequestDto(
    val refreshToken: String
)

// Модель данных, общая для слоёв этого модуля.
data class AuthUserDto(
    val id: String? = null,
    val email: String? = null,
    val bridgeUserKey: String? = null
)

// Модель данных для ответов, возвращаемых backend.
data class AuthResponseDto(
    @SerializedName(value = "accessToken", alternate = ["token"])
    val accessToken: String,
    val refreshToken: String,
    val expiresInSeconds: Long? = null,
    val refreshExpiresInSeconds: Long? = null,
    val email: String? = null,
    val bridgeUserKey: String? = null,
    val user: AuthUserDto? = null
) {
    // Определяет email из плоских или вложенных полей ответа.
    fun resolvedEmail(): String? = email ?: user?.email
}

// Модель данных для ответов, возвращаемых backend.
data class MeResponseDto(
    val id: String,
    val email: String,
    val bridgeUserKey: String? = null
)
