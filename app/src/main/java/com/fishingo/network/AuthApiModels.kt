package com.fishingo.network

// Must match backend RegisterRequest DTO
data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String
)

// Must match backend LoginRequest DTO
data class LoginRequest(
    val email: String,
    val password: String
)

// Must match backend UserResponse DTO
data class UserResponse(
    val id: Int,
    val username: String,
    val email: String
)