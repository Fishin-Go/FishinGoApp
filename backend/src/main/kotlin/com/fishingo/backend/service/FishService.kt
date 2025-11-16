package com.fishingo.backend.service

import com.fishingo.backend.model.Fish
import com.fishingo.backend.repository.FishRepository

class FishService(private val fishRepository: FishRepository) {

    fun getAllFish(): List<Fish> = fishRepository.getAll()

    fun getFishById(id: Int): Fish? = fishRepository.getById(id)

    fun createFish(fish: Fish): Fish {
        require(fish.name.isNotBlank()) { "Fish name cannot be empty" }
        return fishRepository.create(fish)
    }

    fun deleteFish(id: Int): Boolean = fishRepository.delete(id)
}
