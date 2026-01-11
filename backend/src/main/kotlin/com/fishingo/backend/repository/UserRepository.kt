package com.fishingo.backend.repository

import com.fishingo.backend.model.User
import com.fishingo.backend.model.UserTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
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

    /**
     * Updates the user row in the users table.
     * Returns the updated user, or null if no row was updated (user not found).
     */
    fun update(id: Int, username: String, email: String, passwordHash: String): User? = transaction {
        val updatedCount = UserTable.update({ UserTable.id eq id }) {
            it[UserTable.username] = username
            it[UserTable.email] = email
            it[UserTable.passwordHash] = passwordHash
        }

        if (updatedCount == 0) return@transaction null

        // Return the updated user
        UserTable
            .select { UserTable.id eq id }
            .singleOrNull()
            ?.let { rowToUser(it) }
    }

    private fun rowToUser(row: ResultRow): User =
        User(
            id = row[UserTable.id],
            username = row[UserTable.username],
            email = row[UserTable.email],
            passwordHash = row[UserTable.passwordHash]
        )
}
