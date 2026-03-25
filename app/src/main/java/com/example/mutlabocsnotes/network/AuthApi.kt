package com.example.mutlabocsnotes.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface AuthApi {

    @POST("auth/register")
    suspend fun register(
        @Body request: AuthCredentialsDto
    ): AuthResponseDto

    @POST("auth/login")
    suspend fun login(
        @Body request: AuthCredentialsDto
    ): AuthResponseDto

    @POST("auth/refresh")
    suspend fun refresh(
        @Body request: RefreshTokenRequestDto
    ): AuthResponseDto

    @GET("auth/me")
    suspend fun me(
        @Header("Authorization") authorization: String
    ): MeResponseDto
}
