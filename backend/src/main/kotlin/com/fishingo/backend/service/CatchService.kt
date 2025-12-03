package com.fishingo.backend.service

import com.fishingo.backend.dto.CatchResponse
import com.fishingo.backend.dto.NewCatchRequest
import com.fishingo.backend.model.CatchEntity
import com.fishingo.backend.repository.CatchRepository
import java.time.format.DateTimeFormatter

class CatchService(
    private val catchRepository: CatchRepository = CatchRepository()
) {

    // Format LocalDateTime -> String for the API
    private val formatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    /**
     * Create a new catch for the given user and return it as a CatchResponse.
     */
    fun createCatch(userId: Int, request: NewCatchRequest): CatchResponse {
        val entity = catchRepository.createCatch(userId, request)
        return entity.toResponse()
    }

    /**
     * Get all catches for a user as a list of CatchResponse objects.
     */
    fun getCatchesForUser(userId: Int): List<CatchResponse> {
        return catchRepository.getCatchesForUser(userId).map { it.toResponse() }
    }

    // Helper: convert internal entity -> DTO for the API
    private fun CatchEntity.toResponse(): CatchResponse =
        CatchResponse(
            id = id,
            fishName = fishName,
            region = region,
            locationName = locationName,
            latitude = latitude,
            longitude = longitude,
            description = description,
            dateCaught = dateCaught.format(formatter)
        )
}
