package com.fishingo.backend.dto

data class CatchResponse(
    val id: Int,
    val fishName: String,
    val region: String,
    val locationName: String?,
    val latitude: Double?,
    val longitude: Double?,
    val description: String?,
    val dateCaught: String
)
