package com.example.mutlabocsnotes.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

// Retrofit contract with backend endpoints for this feature.
interface AuthApi {

    // Registers a user and updates local auth state.
    @POST("auth/register")
    suspend fun register(
        @Body request: AuthCredentialsDto
    ): AuthResponseDto

    // Authenticates the user and updates local auth state.
    @POST("auth/login")
    suspend fun login(
        @Body request: AuthCredentialsDto
    ): AuthResponseDto

    // Requests a new access token pair using a refresh token.
    @POST("auth/refresh")
    suspend fun refresh(
        @Body request: RefreshTokenRequestDto
    ): AuthResponseDto

    // Fetches profile information for the current authenticated user.
    @GET("auth/me")
    suspend fun me(
        @Header("Authorization") authorization: String
    ): MeResponseDto
}
