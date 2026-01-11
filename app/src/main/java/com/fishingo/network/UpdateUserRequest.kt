package com.fishingo.network


data class UpdateUserRequest(
    val username: String? =null,
    val email: String? =null,
    val currentPassword: String? = null,
    val newPassword: String? = null
)