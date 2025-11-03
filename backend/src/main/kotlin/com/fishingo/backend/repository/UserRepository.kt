package com.fishingo.backend.repository

import com.fishingo.backend.model.User
import com.fishingo.backend.model.UserTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

class UserRepository {

    fun getAll(): List<User> = transaction {
        UserTable.selectAll().map { row: ResultRow ->
            User(
                id = row[UserTable.id],
                username = row[UserTable.username],
                email = row[UserTable.email],
                password = row[UserTable.password]
            )
        }
    }

    fun getById(id: Int): User? = transaction {
        UserTable.select { UserTable.id eq id }
            .map { row: ResultRow ->
                User(row[UserTable.id], row[UserTable.username], row[UserTable.email], row[UserTable.password])
            }
            .singleOrNull()
    }

    fun create(user: User): User = transaction {
        // PostgreSQL supports RETURNING – Exposed exposes it as insertReturning(...)
        val inserted: ResultRow = UserTable.insertReturning(
            listOf(UserTable.id, UserTable.username, UserTable.email, UserTable.password)
        ) {
            it[username] = user.username
            it[email] = user.email
            it[password] = user.password
        }.single()

        User(
            id = inserted[UserTable.id],
            username = inserted[UserTable.username],
            email = inserted[UserTable.email],
            password = inserted[UserTable.password]
        )
    }

    fun delete(id: Int): Boolean = transaction {
        UserTable.deleteWhere { UserTable.id eq id } > 0
    }
}
