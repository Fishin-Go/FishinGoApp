package com.fishingo.backend.service

import com.fishingo.backend.model.Catch
import com.fishingo.backend.repository.CatchRepository

class CatchService(private val catchRepository: CatchRepository) {

    fun getAllCatches(): List<Catch> = catchRepository.getAll()

    fun getCatchById(id: Int): Catch? = catchRepository.getById(id)

    fun createCatch(catch: Catch): Catch {
        require(catch.weight > 0) { "Catch weight must be positive" }
        return catchRepository.create(catch)
    }

    fun deleteCatch(id: Int): Boolean = catchRepository.delete(id)
}
