package com.fishingo.backend.repository

import com.fishingo.backend.model.Catch
import com.fishingo.backend.model.CatchTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

class CatchRepository {

    fun getAll(): List<Catch> = transaction {
        CatchTable.selectAll().map { row: ResultRow ->
            Catch(
                id = row[CatchTable.id],
                userId = row[CatchTable.userId],
                fishId = row[CatchTable.fishId],
                weight = row[CatchTable.weight],
                timestamp = row[CatchTable.timestamp]
                )
        }
    }

    fun getById(id: Int): Catch? = transaction {
        CatchTable.select { CatchTable.id eq id }
            .map { row: ResultRow ->
                Catch(
                    id = row[CatchTable.id],
                    userId = row[CatchTable.userId],
                    fishId = row[CatchTable.fishId],
                    weight = row[CatchTable.weight],
                    timestamp = row[CatchTable.timestamp]
                    )
            }
            .singleOrNull()
    }

    fun create(catch: Catch): Catch = transaction {
        val inserted = CatchTable.insertReturning(
            listOf(
                CatchTable.id,
                CatchTable.userId,
                CatchTable.fishId,
                CatchTable.timestamp,
                CatchTable.weight
            )
        ) {
            it[userId] = catch.userId
            it[fishId] = catch.fishId
            it[timestamp] = catch.timestamp
            it[weight] = catch.weight
        }.single()

        Catch(
            id = inserted[CatchTable.id],
            userId = inserted[CatchTable.userId],
            fishId = inserted[CatchTable.fishId],
            weight = inserted[CatchTable.weight],
            timestamp = inserted[CatchTable.timestamp]
            )
    }

    fun delete(id: Int): Boolean = transaction {
        CatchTable.deleteWhere { CatchTable.id eq id } > 0
    }
}
