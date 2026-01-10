package com.fishingo.network

// Request to update user information
data class UpdateUserRequest(
    val username: String,
    val email: String,
    val currentPassword: String? = null,
    val newPassword: String? = null
)