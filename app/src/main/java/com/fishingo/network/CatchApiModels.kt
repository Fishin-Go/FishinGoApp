package com.fishingo.network

// What the app sends to POST /catches
data class NewCatchRequest(
    val fishName: String,
    val region: String,
    val locationName: String?,
    val latitude: Double?,
    val longitude: Double?,
    val description: String?
)

// What the backend returns from POST /catches and GET /catches
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
