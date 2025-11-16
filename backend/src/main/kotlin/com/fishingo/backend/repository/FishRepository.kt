package com.fishingo.backend.repository

import com.fishingo.backend.model.Fish
import com.fishingo.backend.model.FishTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

class FishRepository {

    fun getAll(): List<Fish> = transaction {
        FishTable.selectAll().map { row: ResultRow ->
            Fish(
                id = row[FishTable.id],
                name = row[FishTable.name],
                rarity = row[FishTable.rarity],
                waterBodyId = row[FishTable.waterBodyId]
            )
        }
    }

    fun getById(id: Int): Fish? = transaction {
        FishTable.select { FishTable.id eq id }
            .map { row: ResultRow ->
                Fish(
                    id = row[FishTable.id],
                    name = row[FishTable.name],
                    rarity = row[FishTable.rarity],
                    waterBodyId = row[FishTable.waterBodyId]
                )
            }
            .singleOrNull()
    }

    fun create(fish: Fish): Fish = transaction {
        val inserted = FishTable.insertReturning(
            listOf(FishTable.id, FishTable.name, FishTable.rarity, FishTable.waterBodyId)
        ) {
            it[name] = fish.name
            it[rarity] = fish.rarity
            it[waterBodyId] = fish.waterBodyId
        }.single()

        Fish(
            id = inserted[FishTable.id],
            name = inserted[FishTable.name],
            rarity = inserted[FishTable.rarity],
            waterBodyId = inserted[FishTable.waterBodyId]
        )
    }

    fun delete(id: Int): Boolean = transaction {
        FishTable.deleteWhere { FishTable.id eq id } > 0
    }
}
