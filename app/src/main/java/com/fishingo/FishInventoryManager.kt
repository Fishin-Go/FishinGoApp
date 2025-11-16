package com.fishingo

import android.content.Context
import android.util.Log

// Simple in-memory storage - no persistence for now to avoid crashes
object FishInventoryManager {

    private const val TAG = "FishInventoryManager"

    private lateinit var context: Context
    private var fishList: MutableList<CaughtFish> = mutableListOf()
    private var isInitialized = false

    fun initialize(appContext: Context) {
        try {
            context = appContext
            isInitialized = true
            Log.d(TAG, "Initialized successfully (in-memory mode)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize", e)
            fishList = mutableListOf()
            isInitialized = true
        }
    }

    fun addFish(caughtFish: CaughtFish) {
        checkInitialized()
        try {
            fishList.add(caughtFish)
            Log.d(TAG, "Fish added: ${caughtFish.fish.species} (Total: ${fishList.size})")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add fish", e)
            throw e
        }
    }

    fun getAllFish(): List<CaughtFish> {
        checkInitialized()
        return fishList.toList()
    }

    fun clear() {
        checkInitialized()
        try {
            fishList.clear()
            Log.d(TAG, "All fish cleared")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear fish", e)
            throw e
        }
    }

    private fun checkInitialized() {
        if (!isInitialized) {
            throw IllegalStateException("FishInventoryManager not initialized. Call initialize() first.")
        }
    }
}