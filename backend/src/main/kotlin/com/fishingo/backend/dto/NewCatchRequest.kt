package com.fishingo.backend.dto

import kotlinx.serialization.Serializable

@Serializable
data class NewCatchRequest(
    val fishName: String,
    val region: String,
    val locationName: String?,
    val latitude: Double?,
    val longitude: Double?,
    val description: String?
)
