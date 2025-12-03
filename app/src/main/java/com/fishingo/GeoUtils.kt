package com.fishingo

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

/**
 * Geo helper functions:
 * - getRegionForLocation: lat/lon -> county -> macro-region
 * - findNearbyWaterBody: lat/lon -> any water feature within radius (Overpass)
 * - getWaterBodyNameForLocation: optional, single-point water name via Nominatim
 */

// -----------------------------------------------------------------------------
// Public API
// -----------------------------------------------------------------------------

/**
 * Return macro-region name ("Transilvania", "Moldova", etc.) for a location,
 * using Nominatim -> county -> hardcoded county->region mapping.
 */
suspend fun getRegionForLocation(
    context: Context,
    latitude: Double,
    longitude: Double
): String? {
    val address: JsonObject? = withContext(Dispatchers.IO) {
        getAddressFromNominatim(latitude, longitude)
    }

    if (address == null) {
        Log.w("GeoUtils", "getRegionForLocation: address is null")
        return null
    }

    val countyRaw = address["county"]?.asString
    if (countyRaw.isNullOrBlank()) {
        Log.w("GeoUtils", "getRegionForLocation: county missing in address: $address")
        return null
    }

    val countyNorm = normalizeCountyName(countyRaw)
    val region = mapCountyToRegion(countyNorm)

    Log.d("GeoUtils", "County '$countyRaw' (normalized: '$countyNorm') mapped to region '$region'")
    return region
}

/**
 * Optional: try to get water body name (river/lake/etc.) exactly at the point.
 * Not used for 50m detection, but kept for debugging.
 */
suspend fun getWaterBodyNameForLocation(
    context: Context,
    latitude: Double,
    longitude: Double
): String? {
    val address: JsonObject? = withContext(Dispatchers.IO) {
        getAddressFromNominatim(latitude, longitude)
    }

    if (address == null) {
        Log.e("GeoUtils", "getWaterBodyNameForLocation: address is NULL for lat=$latitude lon=$longitude")
        return null
    }

    Log.d("GeoUtils", "Reverse-geocode full address JSON:\n$address")

    val waterKeys = listOf("water", "river", "lake", "reservoir", "canal", "sea")
    for (key in waterKeys) {
        val value = address[key]?.asString
        if (!value.isNullOrBlank()) {
            Log.d("GeoUtils", "Detected water body at point: key=$key value=$value")
            return value
        }
    }

    Log.w("GeoUtils", "No water-related fields found in Nominatim address JSON.")
    return null
}

/**
 * Result of a nearby water search using Overpass API.
 */
data class NearbyWaterBody(
    val name: String?,          // e.g. "Someșul Mic"
    val type: String?,          // e.g. "river", "lake"
    val distanceMeters: Double? // approximate distance from query point
)

/**
 * Query Overpass API for any water feature within [radiusMeters] of lat/lon.
 * Returns the first match found, or null if nothing is mapped nearby.
 */
suspend fun findNearbyWaterBody(
    context: Context,
    latitude: Double,
    longitude: Double,
    radiusMeters: Int = 50
): NearbyWaterBody? = withContext(Dispatchers.IO) {

    val query = """
        [out:json][timeout:10];
        (
          way(around:$radiusMeters,$latitude,$longitude)["water"];
          way(around:$radiusMeters,$latitude,$longitude)["waterway"];
          way(around:$radiusMeters,$latitude,$longitude)["natural"="water"];
          relation(around:$radiusMeters,$latitude,$longitude)["water"];
          relation(around:$radiusMeters,$latitude,$longitude)["waterway"];
          relation(around:$radiusMeters,$latitude,$longitude)["natural"="water"];
        );
        out center 1;
    """.trimIndent()

    val url = URL("https://overpass-api.de/api/interpreter")
    val conn = (url.openConnection() as HttpURLConnection).apply {
        requestMethod = "POST"
        doOutput = true
        setRequestProperty(
            "User-Agent",
            "FishinGo/1.0 (cosminserban2003@gmail.com)"
        )
        setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
    }

    try {
        val encoded = "data=" + URLEncoder.encode(query, "UTF-8")
        conn.outputStream.use { out ->
            out.write(encoded.toByteArray())
        }

        val code = conn.responseCode
        if (code != HttpURLConnection.HTTP_OK) {
            Log.w("GeoUtils", "Overpass HTTP $code")
            return@withContext null
        }

        val jsonText = conn.inputStream.bufferedReader().use { it.readText() }
        Log.d("GeoUtils", "Overpass raw JSON: $jsonText")

        val gson = Gson()
        val root = gson.fromJson(jsonText, JsonObject::class.java)
        val elements = root["elements"]?.asJsonArray ?: return@withContext null
        if (elements.size() == 0) {
            Log.d("GeoUtils", "No water elements within $radiusMeters m")
            return@withContext null
        }

        val elem = elements[0].asJsonObject
        val tags = elem["tags"]?.asJsonObject

        val name = tags?.get("name")?.asString
        val waterType =
            tags?.get("water")?.asString
                ?: tags?.get("waterway")?.asString
                ?: tags?.get("natural")?.asString

        val center = elem["center"]?.asJsonObject
        val waterLat = center?.get("lat")?.asDouble
        val waterLon = center?.get("lon")?.asDouble

        var distance: Double? = null
        if (waterLat != null && waterLon != null) {
            val result = FloatArray(1)
            android.location.Location.distanceBetween(
                latitude, longitude,
                waterLat, waterLon,
                result
            )
            distance = result[0].toDouble()
            Log.d("GeoUtils", "Nearest water distance ≈ ${distance} m")
        }

        NearbyWaterBody(
            name = name,
            type = waterType,
            distanceMeters = distance
        )
    } catch (e: Exception) {
        Log.e("GeoUtils", "Error calling Overpass", e)
        null
    } finally {
        conn.disconnect()
    }
}

// -----------------------------------------------------------------------------
// Internal helpers
// -----------------------------------------------------------------------------

/**
 * Reverse-geocode a point using Nominatim (OpenStreetMap).
 * Returns the "address" JSON object or null if call failed.
 */
private fun getAddressFromNominatim(
    latitude: Double,
    longitude: Double
): JsonObject? {
    return try {
        val url = URL(
            "https://nominatim.openstreetmap.org/reverse" +
                    "?lat=$latitude&lon=$longitude&format=jsonv2&addressdetails=1"
        )
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty(
                "User-Agent",
                "FishinGo/1.0 (cosminserban2003@gmail.com)"
            )
        }

        val code = conn.responseCode
        if (code != HttpURLConnection.HTTP_OK) {
            Log.w("GeoUtils", "Nominatim HTTP $code")
            conn.disconnect()
            return null
        }

        val jsonText = conn.inputStream.bufferedReader().use { it.readText() }
        conn.disconnect()

        val gson = Gson()
        val root = gson.fromJson(jsonText, JsonObject::class.java)
        val address = root["address"]?.asJsonObject

        Log.d("GeoUtils", "Nominatim address JSON: $address")
        address
    } catch (e: Exception) {
        Log.e("GeoUtils", "Error calling Nominatim", e)
        null
    }
}

/**
 * Normalize county names so we can match regardless of diacritics / prefixes.
 */
private fun normalizeCountyName(raw: String): String {
    var s = raw.trim().lowercase(Locale.getDefault())

    s = s.replace("județul", "")
        .replace("judetul", "")
        .replace("county", "")
        .trim()

    s = s.replace("ă", "a")
        .replace("â", "a")
        .replace("î", "i")
        .replace("ș", "s")
        .replace("ş", "s")
        .replace("ț", "t")
        .replace("ţ", "t")

    s = s.replace(Regex("\\s+"), " ")
    return s
}

/**
 * Hardcoded mapping from normalized county name to macro-region.
 */
private fun mapCountyToRegion(normalizedCounty: String): String? {
    val transilvania = setOf(
        "alba", "arad", "bihor", "bistrita-nasaud", "brasov",
        "cluj", "covasna", "harghita", "hunedoara", "mures", "salaj"
    )

    val moldova = setOf(
        "bacau", "botosani", "iasi", "neamt", "suceava", "vaslui", "vrancea", "galati"
    )

    val muntenia = setOf(
        "arges", "braila", "buzau", "calarasi", "dambovita", "giurgiu",
        "ialomita", "ilfov", "prahova", "teleorman"
    )

    val dobrogea = setOf(
        "constanta", "tulcea"
    )

    val banat = setOf(
        "timis", "caras-severin"
    )

    val oltenia = setOf(
        "dolj", "gorj", "mehedinti", "olt", "valcea"
    )

    val bucharestNames = setOf(
        "bucuresti", "bucharest"
    )

    return when {
        normalizedCounty in transilvania -> "Transilvania"
        normalizedCounty in moldova -> "Moldova"
        normalizedCounty in muntenia || normalizedCounty in bucharestNames -> "Muntenia"
        normalizedCounty in dobrogea -> "Dobrogea"
        normalizedCounty in banat -> "Banat"
        normalizedCounty in oltenia -> "Oltenia"
        else -> null
    }
}
