package com.fishingo

data class User(
    val id: String,
    val username: String,
    val email: String,
    val profilePictureUrl: String? = null
)

