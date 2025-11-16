package com.fishingo.backend.model

import org.jetbrains.exposed.sql.Table

object CatchTable : Table("catches") {
    val id = integer("id").autoIncrement()
    val userId = integer("user_id").references(UserTable.id)
    val fishId = integer("fish_id").references(FishTable.id)
    val weight = double("weight")
    val timestamp = varchar("timestamp", 50)
    override val primaryKey = PrimaryKey(id)
}

data class Catch(
    val id: Int? = null,
    val userId: Int,
    val fishId: Int,
    val weight: Double,
    val timestamp: String
)
