package com.fishingo.backend.dto

import kotlinx.serialization.Serializable

@Serializable
data class UpdateUserRequest(
    val username: String,
    val email: String,
    val currentPassword: String? = null,
    val newPassword: String? = null
)