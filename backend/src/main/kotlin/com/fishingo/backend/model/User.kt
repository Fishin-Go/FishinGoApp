package com.fishingo.backend.model

import org.jetbrains.exposed.sql.Table

object UserTable : Table("users") {
    val id = integer("id").autoIncrement()
    val username = varchar("username", 50)
    val email = varchar("email", 100)
    val passwordHash = varchar("password_hash", 100)

    override val primaryKey = PrimaryKey(id)
}

data class User(
    val id: Int? = null,
    val username: String,
    val email: String,
    val passwordHash: String
)
