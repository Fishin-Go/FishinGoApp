package com.fishingo.backend.service

import com.fishingo.backend.model.User
import com.fishingo.backend.repository.UserRepository

class UserService(private val userRepository: UserRepository) {

    fun getAllUsers(): List<User> = userRepository.getAll()

    fun getUserById(id: Int): User? = userRepository.getById(id)

    fun createUser(user: User): User {
        require(user.username.isNotBlank()) { "username cannot be empty" }
        return userRepository.create(user)
    }

    fun deleteUser(id: Int): Boolean = userRepository.delete(id)
}
