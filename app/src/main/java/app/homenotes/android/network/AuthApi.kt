package app.homenotes.android.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.Response

interface AuthApi {

    @POST("auth/register")
    suspend fun register(
        @Body request: AuthCredentialsDto
    ): AuthResponseDto

    @POST("auth/login")
    suspend fun login(
        @Body request: AuthCredentialsDto
    ): AuthResponseDto

    @POST("auth/social/google")
    suspend fun loginWithGoogle(
        @Body request: GoogleSocialLoginRequestDto
    ): AuthResponseDto

    @POST("auth/social/yandex")
    suspend fun loginWithYandex(
        @Body request: YandexSocialLoginRequestDto
    ): AuthResponseDto

    @POST("auth/refresh")
    suspend fun refresh(
        @Body request: RefreshTokenRequestDto
    ): AuthResponseDto

    @POST("auth/logout")
    suspend fun logout(
        @Body request: LogoutRequestDto
    ): Response<Unit>

    @GET("auth/me")
    suspend fun me(
        @Header("Authorization") authorization: String
    ): MeResponseDto
}
