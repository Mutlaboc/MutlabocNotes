package app.homenotes.android.network

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable
data class AuthCredentialsDto(
    val email: String,
    val password: String
)

@Serializable
data class GoogleSocialLoginRequestDto(
    val idToken: String
)

@Serializable
data class YandexSocialLoginRequestDto(
    val accessToken: String
)

@Serializable
data class RefreshTokenRequestDto(
    val refreshToken: String
)

@Serializable
data class LogoutRequestDto(
    val refreshToken: String
)

@Serializable
data class AuthUserDto(
    val id: String? = null,
    val email: String? = null,
    val bridgeUserKey: String? = null
)

// Wire shape as it actually comes off the backend: some responses use "token" instead of
// "accessToken". Kept private/raw so AuthResponseDtoSerializer can map it onto the public DTO.
@Serializable
private data class AuthResponseDtoRaw(
    val accessToken: String? = null,
    val token: String? = null,
    val refreshToken: String,
    val expiresInSeconds: Long? = null,
    val refreshExpiresInSeconds: Long? = null,
    val email: String? = null,
    val bridgeUserKey: String? = null,
    val user: AuthUserDto? = null
)

object AuthResponseDtoSerializer : KSerializer<AuthResponseDto> {
    private val delegate = AuthResponseDtoRaw.serializer()
    override val descriptor = delegate.descriptor

    override fun deserialize(decoder: Decoder): AuthResponseDto {
        val raw = decoder.decodeSerializableValue(delegate)
        val accessToken = requireNotNull(raw.accessToken ?: raw.token) {
            "Auth response is missing both accessToken and token"
        }
        return AuthResponseDto(
            accessToken = accessToken,
            refreshToken = raw.refreshToken,
            expiresInSeconds = raw.expiresInSeconds,
            refreshExpiresInSeconds = raw.refreshExpiresInSeconds,
            email = raw.email,
            bridgeUserKey = raw.bridgeUserKey,
            user = raw.user,
        )
    }

    override fun serialize(encoder: Encoder, value: AuthResponseDto) {
        encoder.encodeSerializableValue(
            delegate,
            AuthResponseDtoRaw(
                accessToken = value.accessToken,
                refreshToken = value.refreshToken,
                expiresInSeconds = value.expiresInSeconds,
                refreshExpiresInSeconds = value.refreshExpiresInSeconds,
                email = value.email,
                bridgeUserKey = value.bridgeUserKey,
                user = value.user,
            )
        )
    }
}

@Serializable(with = AuthResponseDtoSerializer::class)
data class AuthResponseDto(
    val accessToken: String,
    val refreshToken: String,
    val expiresInSeconds: Long? = null,
    val refreshExpiresInSeconds: Long? = null,
    val email: String? = null,
    val bridgeUserKey: String? = null,
    val user: AuthUserDto? = null
) {
    fun resolvedEmail(): String? = email ?: user?.email
}

@Serializable
data class MeResponseDto(
    val id: String,
    val email: String,
    val bridgeUserKey: String? = null
)
