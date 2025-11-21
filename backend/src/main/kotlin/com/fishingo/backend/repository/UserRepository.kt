package com.fishingo.backend.repository

import com.fishingo.backend.model.User
import com.fishingo.backend.model.UserTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

class UserRepository {

    fun getAll(): List<User> = transaction {
        UserTable.selectAll().map { rowToUser(it) }
    }

    fun findById(id: Int): User? = transaction {
        UserTable
            .select { UserTable.id eq id }
            .singleOrNull()
            ?.let { rowToUser(it) }
    }

    fun findByEmail(email: String): User? = transaction {
        UserTable
            .select { UserTable.email eq email }
            .singleOrNull()
            ?.let { rowToUser(it) }
    }

    fun create(username: String, email: String, passwordHash: String): User = transaction {
        val generatedId = UserTable.insert { row ->
            row[UserTable.username] = username
            row[UserTable.email] = email
            row[UserTable.passwordHash] = passwordHash
        } get UserTable.id

        User(
            id = generatedId,
            username = username,
            email = email,
            passwordHash = passwordHash
        )
    }

    private fun rowToUser(row: ResultRow): User =
        User(
            id = row[UserTable.id],
            username = row[UserTable.username],
            email = row[UserTable.email],
            passwordHash = row[UserTable.passwordHash]
        )
}
