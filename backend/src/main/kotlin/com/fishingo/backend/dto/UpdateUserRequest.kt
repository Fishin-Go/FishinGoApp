package com.fishingo.backend.dto

import kotlinx.serialization.Serializable

@Serializable
data class UpdateUserRequest(
    val username: String? = null,
    val email: String? = null,
    val currentPassword: String? = null,
    val newPassword: String? = null
)