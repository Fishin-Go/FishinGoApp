package com.fishingo.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


object ApiClient {

    // IMPORTANT:
    // - If you're running the backend on your PC and the app in the **Android emulator**,
    //   use 10.0.2.2 to access your computer's localhost.
    // - If you're using a physical phone on the same Wi-Fi, replace this with your PC's LAN IP,
    //   e.g. "http://192.168.0.23:8080/"

    private const val BASE_URL = "https://fishingo.onrender.com"

    val authApi: AuthApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AuthApi::class.java)
    }
}