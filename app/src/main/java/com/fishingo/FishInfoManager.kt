package com.fishingo

import android.content.Context
import androidx.annotation.DrawableRes
import com.fishingo.R
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

// Now includes the description field
data class FishInfo(
    val latin: String,
    val image: String,
    val description: String
)

object FishInfoManager {

    private val fishMap = mutableMapOf<String, FishInfo>()
    private var isLoaded = false

    fun load(context: Context) {
        if (isLoaded) return

        val inputStream = context.resources.openRawResource(R.raw.fish_info)
        val json = inputStream.bufferedReader().use { it.readText() }

        val type = object : TypeToken<Map<String, FishInfo>>() {}.type
        val parsed: Map<String, FishInfo> = Gson().fromJson(json, type)

        // normalize JSON keys before storing them
        for ((name, info) in parsed) {
            val key = normalizeName(name)
            fishMap[key] = info
        }

        isLoaded = true
    }

    /**
     * Returns the FishInfo for a given fish name from region_fish.json.
     * Works regardless of diacritics, spaces, or hyphens.
     */
    fun getInfo(fishName: String): FishInfo? {
        val key = normalizeName(fishName)
        return fishMap[key]
    }

    /**
     * Converts "babusca.png" → R.drawable.babusca
     * Requires the file to be in res/drawable/
     */
    @DrawableRes
    fun getDrawableId(context: Context, imageName: String): Int {
        val clean = imageName.removeSuffix(".png")
        return context.resources.getIdentifier(clean, "drawable", context.packageName)
    }

    // ---------------- helper ----------------

    private fun normalizeName(name: String): String =
        name.lowercase()
            .replace("ă", "a")
            .replace("â", "a")
            .replace("î", "i")
            .replace("ș", "s").replace("ş", "s")
            .replace("ț", "t").replace("ţ", "t")
            .replace("é", "e")
            .replace(" ", "")
            .replace("-", "")
}
