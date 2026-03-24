package com.example.mutlabocsnotes.network

import com.google.gson.annotations.SerializedName

data class AuthCredentialsDto(
    val email: String,
    val password: String
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

    val email: String? = null,
    val firebaseUid: String? = null,
    val bridgeUserKey: String? = null,
    val user: AuthUserDto? = null
) {
    fun resolvedEmail(): String? = email ?: user?.email

    fun resolvedBridgeUserKey(): String? {
        return bridgeUserKey
            ?: firebaseUid
            ?: user?.bridgeUserKey
            ?: user?.firebaseUid
    }
}

data class MeResponseDto(
    val id: String,
    val email: String,
    val firebaseUid: String? = null,
    val bridgeUserKey: String? = null
) {
    fun resolvedBridgeUserKey(): String? = bridgeUserKey ?: firebaseUid
}