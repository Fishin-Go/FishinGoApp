package com.fishingo.backend.repository

import com.fishingo.backend.model.WaterBody
import com.fishingo.backend.model.WaterBodyTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

class WaterBodyRepository {

    fun getAll(): List<WaterBody> = transaction {
        WaterBodyTable.selectAll().map { row: ResultRow ->
            WaterBody(
                id = row[WaterBodyTable.id],
                name = row[WaterBodyTable.name],
                type = row[WaterBodyTable.type],
                latitude = row[WaterBodyTable.latitude],
                longitude = row[WaterBodyTable.longitude]
            )
        }
    }

    fun getById(id: Int): WaterBody? = transaction {
        WaterBodyTable.select { WaterBodyTable.id eq id }
            .map { row: ResultRow ->
                WaterBody(
                    id = row[WaterBodyTable.id],
                    name = row[WaterBodyTable.name],
                    type = row[WaterBodyTable.type],
                    latitude = row[WaterBodyTable.latitude],
                    longitude = row[WaterBodyTable.longitude]
                )
            }
            .singleOrNull()
    }

    fun create(waterBody: WaterBody): WaterBody = transaction {
        val inserted = WaterBodyTable.insertReturning(
            listOf(
                WaterBodyTable.id,
                WaterBodyTable.name,
                WaterBodyTable.latitude,
                WaterBodyTable.longitude
            )
        ) {
            it[name] = waterBody.name
            it[latitude] = waterBody.latitude
            it[longitude] = waterBody.longitude
        }.single()

        WaterBody(
            id = inserted[WaterBodyTable.id],
            name = inserted[WaterBodyTable.name],
            type = inserted[WaterBodyTable.type],
            latitude = inserted[WaterBodyTable.latitude],
            longitude = inserted[WaterBodyTable.longitude]
        )
    }

    fun delete(id: Int): Boolean = transaction {
        WaterBodyTable.deleteWhere { WaterBodyTable.id eq id } > 0
    }
}
