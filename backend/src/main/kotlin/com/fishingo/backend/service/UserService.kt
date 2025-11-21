package com.fishingo.backend.service

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
        // 2) Hash password
        val hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt())

        // 3) Save user with hashed password
        return userRepository.create(
            username = username,
            email = email,
            passwordHash = hashedPassword
        )
    }

    fun login(email: String, password: String): User? {
        val user = userRepository.findByEmail(email)?: return null

        val matches = BCrypt.checkpw(password,user.passwordHash)
        return if (matches) user else null
    }

    fun getUser(id: Int): User? = userRepository.findById(id)

    fun getAllUsers(): List<User> = userRepository.getAll()
}
