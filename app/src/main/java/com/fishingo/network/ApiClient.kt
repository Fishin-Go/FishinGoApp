package com.fishingo.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {

    // IMPORTANT: must end with '/' for Retrofit
    private const val BASE_URL = "https://fishingo.onrender.com/"

    // Single Retrofit instance
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val authApi: AuthApi by lazy {
        retrofit.create(AuthApi::class.java)
    }

    val catchApi: CatchApi by lazy {
        retrofit.create(CatchApi::class.java)
    }
}
