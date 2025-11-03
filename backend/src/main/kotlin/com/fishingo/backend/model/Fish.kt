package com.fishingo.backend.model

import org.jetbrains.exposed.sql.Table

object FishTable : Table("fish") {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 100)
    val rarity = varchar("rarity", 50)
    val waterBodyId = integer("water_body_id").references(WaterBodyTable.id)
    override val primaryKey = PrimaryKey(id)
}

data class Fish(
    val id: Int? = null,
    val name: String,
    val rarity: String,
    val waterBodyId: Int
)