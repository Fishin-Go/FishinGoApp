package com.fishingo.backend.repository

import com.fishingo.backend.dto.NewCatchRequest
import com.fishingo.backend.model.CatchEntity
import com.fishingo.backend.model.FishCatchTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

class CatchRepository {

    /**
     * Insert a new catch for a given user into the fish_catches table
     * and return the created CatchEntity.
     */
    fun createCatch(userId: Int, request: NewCatchRequest): CatchEntity = transaction {
        val now = LocalDateTime.now()

        val generatedId = FishCatchTable.insert { row ->
            row[FishCatchTable.userId] = userId
            row[fishName] = request.fishName
            row[region] = request.region
            row[locationName] = request.locationName
            row[latitude] = request.latitude
            row[longitude] = request.longitude
            row[dateCaught] = now
            row[description] = request.description
        } get FishCatchTable.id

        CatchEntity(
            id = generatedId,
            userId = userId,
            fishName = request.fishName,
            region = request.region,
            locationName = request.locationName,
            latitude = request.latitude,
            longitude = request.longitude,
            dateCaught = now,
            description = request.description
        )
    }

    /**
     * Get all catches for the given user, newest first.
     */
    fun getCatchesForUser(userId: Int): List<CatchEntity> = transaction {
        FishCatchTable
            .select { FishCatchTable.userId eq userId }
            .orderBy(FishCatchTable.dateCaught to SortOrder.DESC)
            .map { rowToEntity(it) }
    }

    // Helper to convert a DB row into our Kotlin entity
    private fun rowToEntity(row: ResultRow): CatchEntity =
        CatchEntity(
            id = row[FishCatchTable.id],
            userId = row[FishCatchTable.userId],
            fishName = row[FishCatchTable.fishName],
            region = row[FishCatchTable.region],
            locationName = row[FishCatchTable.locationName],
            latitude = row[FishCatchTable.latitude],
            longitude = row[FishCatchTable.longitude],
            dateCaught = row[FishCatchTable.dateCaught],
            description = row[FishCatchTable.description]
        )
}
