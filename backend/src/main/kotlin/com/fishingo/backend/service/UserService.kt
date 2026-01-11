package com.fishingo.backend.service

import com.fishingo.backend.dto.UpdateUserRequest
import com.fishingo.backend.dto.UserResponse
import com.fishingo.backend.model.User
import com.fishingo.backend.repository.UserRepository
import org.mindrot.jbcrypt.BCrypt

class UserService(
    private val userRepository: UserRepository = UserRepository()
) {
    /**
     * Register a new user.
     * - Returns the created user if success.
     * - Returns null if email is already taken.
     */
    fun register(username: String, email: String, password: String): User? {
        val existing = userRepository.findByEmail(email)
        if (existing != null) {
            return null // email already used
        }
        // Hash password
        val hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt())

        // Save user with hashed password
        return userRepository.create(
            username = username,
            email = email,
            passwordHash = hashedPassword
        )
    }

    fun login(email: String, password: String): User? {
        val user = userRepository.findByEmail(email) ?: return null

        val matches = BCrypt.checkpw(password, user.passwordHash)
        return if (matches) user else null
    }

    fun getUser(id: Int): User? = userRepository.findById(id)

    fun getAllUsers(): List<User> = userRepository.getAll()

    /**
     * Update user information.
     * Throws exceptions for error cases that the controller will handle.
     */
    fun updateUser(userId: Int, request: UpdateUserRequest): UserResponse {
        // Find the user
        val user = userRepository.findById(userId)
            ?: throw IllegalArgumentException("User not found")

        // Decide new username/email (keep existing if not provided)
        val newUsername = request.username?.trim()?.takeIf { it.isNotEmpty() } ?: user.username
        val newEmail = request.email?.trim()?.takeIf { it.isNotEmpty() } ?: user.email

        // If email changed, verify it's not taken
        if (newEmail != user.email) {
            val existingUser = userRepository.findByEmail(newEmail)
            if (existingUser != null && existingUser.id != userId) {
                throw IllegalStateException("Email already in use")
            }
        }

        // Password update logic
        val newPasswordHash = when {
            // If user tries to change password (newPassword provided and not blank)
            !request.newPassword.isNullOrBlank() -> {
                // Must provide current password
                if (request.currentPassword.isNullOrBlank()) {
                    throw IllegalArgumentException("Current password required to change password")
                }

                // Verify current password
                if (!BCrypt.checkpw(request.currentPassword, user.passwordHash)) {
                    throw SecurityException("Current password is incorrect")
                }

                // Hash new password
                BCrypt.hashpw(request.newPassword, BCrypt.gensalt())
            }

            // Otherwise keep existing password
            else -> user.passwordHash
        }

        // Update user in repository
        val updatedUser = userRepository.update(
            id = userId,
            username = newUsername,
            email = newEmail,
            passwordHash = newPasswordHash
        ) ?: throw IllegalArgumentException("Failed to update user")

        return UserResponse(
            id = updatedUser.id!!,
            username = updatedUser.username,
            email = updatedUser.email
        )
    }
}