package com.example.mutlabocsnotes.network

import com.google.gson.annotations.SerializedName

data class AuthCredentialsDto(
    val email: String,
    val password: String
)

data class RefreshTokenRequestDto(
    val refreshToken: String
)

data class AuthUserDto(
    val id: String? = null,
    val email: String? = null,
    val firebaseUid: String? = null,
    val bridgeUserKey: String? = null
)

data class AuthResponseDto(
    @SerializedName(value = "accessToken", alternate = ["token"])
    val accessToken: String,
    val refreshToken: String,
    val expiresInSeconds: Long? = null,
    val refreshExpiresInSeconds: Long? = null,
    val email: String? = null,
    val firebaseUid: String? = null,
    val bridgeUserKey: String? = null,
    val user: AuthUserDto? = null
) {
    fun resolvedEmail(): String? = email ?: user?.email
}

data class MeResponseDto(
    val id: String,
    val email: String,
    val firebaseUid: String? = null,
    val bridgeUserKey: String? = null
)
