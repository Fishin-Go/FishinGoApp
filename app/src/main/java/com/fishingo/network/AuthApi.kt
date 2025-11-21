package com.fishingo.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {

    @POST("/register")
    suspend fun register(
        @Body body: RegisterRequest
    ): Response<UserResponse>

    @POST("/login")
    suspend fun login(
        @Body body: LoginRequest
    ): Response<UserResponse>
}