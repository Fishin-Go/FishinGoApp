package com.fishingo.backend.dto

import kotlinx.serialization.Serializable

@Serializable
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
