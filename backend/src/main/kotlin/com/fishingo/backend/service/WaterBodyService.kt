package com.fishingo.backend.service

import com.fishingo.backend.model.WaterBody
import com.fishingo.backend.repository.WaterBodyRepository

class WaterBodyService(private val waterBodyRepository: WaterBodyRepository) {

    fun getAllWaterBodies(): List<WaterBody> = waterBodyRepository.getAll()

    fun getWaterBodyById(id: Int): WaterBody? = waterBodyRepository.getById(id)

    fun createWaterBody(waterBody: WaterBody): WaterBody {
        require(waterBody.name.isNotBlank()) { "Water body name cannot be empty" }
        require(waterBody.type.isNotBlank()) { "Water body type cannot be empty" }
        return waterBodyRepository.create(waterBody)
    }

    fun deleteWaterBody(id: Int): Boolean = waterBodyRepository.delete(id)
}
