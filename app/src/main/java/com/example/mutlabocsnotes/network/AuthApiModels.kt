package com.example.mutlabocsnotes.network

import com.google.gson.annotations.SerializedName

// Data model shared between layers of this module.
data class AuthCredentialsDto(
    val email: String,
    val password: String
)

// Data model for request payloads sent to the backend.
data class RefreshTokenRequestDto(
    val refreshToken: String
)

// Data model shared between layers of this module.
data class AuthUserDto(
    val id: String? = null,
    val email: String? = null,
    val bridgeUserKey: String? = null
)

// Data model for responses returned by the backend.
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
    // Resolves email from flattened or nested response fields.
    fun resolvedEmail(): String? = email ?: user?.email
}

// Data model for responses returned by the backend.
data class MeResponseDto(
    val id: String,
    val email: String,
    val bridgeUserKey: String? = null
)
