package com.fishingo.backend.model

import org.jetbrains.exposed.sql.Table

object WaterBodyTable : Table("water_bodies") {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 100)
    val type = varchar("type", 50)
    val latitude = double("latitude")
    val longitude = double("longitude")
    override val primaryKey = PrimaryKey(id)
}

data class WaterBody(
    val id: Int? = null,
    val name: String,
    val type: String,
    val latitude: Double,
    val longitude: Double
)
