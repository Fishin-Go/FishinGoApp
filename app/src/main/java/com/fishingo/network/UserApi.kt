package com.fishingo.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.PUT
import retrofit2.http.Path

interface UserApi {

    @PUT("/users/{userId}")
    suspend fun updateUser(
        @Path("userId") userId: Int,
        @Body body: UpdateUserRequest
    ): Response<UserResponse>
}