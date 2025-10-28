package com.fishingo
import java.util.*

// Fish rarity levels
enum class FishRarity(val displayName: String, val color: Long) {
    COMMON("Common", 0xFF757575),
    UNCOMMON("Uncommon", 0xFF4CAF50),
    RARE("Rare", 0xFF2196F3),
    EPIC("Epic", 0xFF9C27B0),
    LEGENDARY("Legendary", 0xFFFF9800)
}

// Water types where fish can be found
enum class WaterType(val displayName: String) {
    RIVER("River"),
    LAKE("Lake"),
    OCEAN("Ocean"),
    POND("Pond"),
    STREAM("Stream")
}

// Fish data class
data class Fish(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val species: String,
    val rarity: FishRarity,
    val waterType: WaterType,
    val weight: Double, // in kg
    val length: Double, // in cm
    val imageUrl: String? = null,
    val description: String = ""
)

// Caught fish - includes catch details
data class CaughtFish(
    val id: String = UUID.randomUUID().toString(),
    val fish: Fish,
    val caughtAt: Long = System.currentTimeMillis(),
    val location: FishLocation,
    val userId: String
)

// Location where fish was caught
data class FishLocation(
    val latitude: Double,
    val longitude: Double,
    val locationName: String = "Unknown Location"
)