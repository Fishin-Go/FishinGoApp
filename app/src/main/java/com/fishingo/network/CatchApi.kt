package com.fishingo.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface CatchApi {

    // POST /catches?userId=123
    @POST("/catches")
    suspend fun createCatch(
        @Query("userId") userId: Int,
        @Body body: NewCatchRequest
    ): Response<CatchResponse>

    // GET /catches?userId=123  (we’ll use this later for Fishnet)
    @GET("/catches")
    suspend fun getCatches(
        @Query("userId") userId: Int
    ): Response<List<CatchResponse>>
}
