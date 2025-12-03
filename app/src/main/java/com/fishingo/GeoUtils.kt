package com.fishingo

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONObject
import java.text.Normalizer
import kotlin.math.*

// -------------------------
// Data classes
// -------------------------

data class NearbyWaterBody(
    val name: String?,
    val type: String?,
    val distanceMeters: Double?
)

// -------------------------
//  REGION (county -> JSON)
// -------------------------

private var cachedCountyRegion: Map<String, String>? = null

private fun loadCountyRegionMap(context: Context): Map<String, String> {
    cachedCountyRegion?.let { return it }

    val inputStream = context.resources.openRawResource(R.raw.county_region)
    val jsonText = inputStream.bufferedReader().use { it.readText() }

    val root = JSONObject(jsonText)
    val result = mutableMapOf<String, String>()

    val keys = root.keys()
    while (keys.hasNext()) {
        val county = keys.next()
        val region = root.getString(county)
        result[normalizeCountyName(county)] = region
    }

    cachedCountyRegion = result
    return result
}

private fun normalizeCountyName(name: String): String {
    var n = Normalizer.normalize(name, Normalizer.Form.NFD)
        .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
        .lowercase()

    n = n.replace("ț", "t")
        .replace("ţ", "t")
        .replace("ș", "s")
        .replace("ş", "s")
        .replace("ă", "a")
        .replace("â", "a")
        .replace("î", "i")

    return n.trim()
}

/**
 * Uses Nominatim reverse geocoding to get the county for a lat/lon,
 * then maps the county to your region (Transilvania, Moldova, etc.)
 * using res/raw/county_region.json.
 */
suspend fun getRegionForLocation(
    context: Context,
    latitude: Double,
    longitude: Double
): String? = withContext(Dispatchers.IO) {
    val TAG = "GeoRegion"

    // ⚠️ IMPORTANT: replace with your real email (you already used one that worked)
    val emailParam = "cosminserban2003@gmail.com"

    val url =
        "https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat=$latitude&lon=$longitude&zoom=10&addressdetails=1&email=$emailParam"

    try {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "FishinGo/1.0 ($emailParam)")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Log.e(TAG, "Nominatim error: ${response.code}")
                return@withContext null
            }

            val jsonText = response.body?.string() ?: return@withContext null
            Log.d(TAG, "Nominatim raw: $jsonText")

            val root = JSONObject(jsonText)
            val address = root.optJSONObject("address") ?: return@withContext null

            val county =
                address.optString("county").takeIf { it.isNotBlank() }
                    ?: address.optString("state").takeIf { it.isNotBlank() }
                    ?: address.optString("region").takeIf { it.isNotBlank() }

            if (county.isNullOrBlank()) {
                Log.w(TAG, "No county/state in Nominatim address")
                return@withContext null
            }

            val normCounty = normalizeCountyName(county)
            val map = loadCountyRegionMap(context)
            val region = map[normCounty]

            Log.d(TAG, "County='$county' (norm='$normCounty') -> region='$region'")
            return@withContext region
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error calling Nominatim", e)
        return@withContext null
    }
}

// ----------------------
//  Water / Overpass
// ----------------------

/**
 * Strict water search with proper river geometry and priority:
 * - Overpass finds water ways whose geometry intersects the [radiusMeters] circle
 * - We compute distance from player to **all geometry points** of each way
 * - We compute a "priority score" (named rivers/lakes > unnamed drains)
 * - We return the best candidate within [radiusMeters]
 */
suspend fun findNearbyWaterBody(
    context: Context,
    latitude: Double,
    longitude: Double,
    radiusMeters: Int
): NearbyWaterBody? = withContext(Dispatchers.IO) {

    val TAG = "OverpassWater"

    val query = """
        [out:json][timeout:25];
        (
          way(around:$radiusMeters,$latitude,$longitude)[water];
          way(around:$radiusMeters,$latitude,$longitude)[waterway];
          way(around:$radiusMeters,$latitude,$longitude)[natural=water];
          way(around:$radiusMeters,$latitude,$longitude)[landuse=reservoir];
        );
        out geom;
    """.trimIndent()

    try {
        val url = "https://overpass-api.de/api/interpreter"
        val body = "data=${query.replace("\n", " ")}"

        val client = OkHttpClient()
        val request = Request.Builder()
            .url(url)
            .post(
                RequestBody.create(
                    "application/x-www-form-urlencoded".toMediaType(),
                    body
                )
            )
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Log.e(TAG, "Overpass error: ${response.code}")
                return@withContext null
            }

            val jsonText = response.body?.string() ?: return@withContext null
            Log.d(TAG, "Overpass raw response: $jsonText")

            val root = JSONObject(jsonText)
            val elements = root.optJSONArray("elements") ?: return@withContext null
            if (elements.length() == 0) {
                Log.d(TAG, "No water elements in ${radiusMeters}m radius")
                return@withContext null
            }

            var bestElement: JSONObject? = null
            var bestDistance = Double.MAX_VALUE
            var bestScore = Int.MIN_VALUE

            for (i in 0 until elements.length()) {
                val el = elements.getJSONObject(i)

                // 1) Find minimal distance from player to this way's geometry
                val geometry = el.optJSONArray("geometry")
                var minDistForWay = Double.MAX_VALUE

                if (geometry != null && geometry.length() > 0) {
                    for (j in 0 until geometry.length()) {
                        val pt = geometry.getJSONObject(j)
                        val wLat = pt.getDouble("lat")
                        val wLon = pt.getDouble("lon")
                        val dist = haversineDistanceMeters(latitude, longitude, wLat, wLon)
                        if (dist < minDistForWay) {
                            minDistForWay = dist
                        }
                    }
                } else {
                    // Fallback: use center if geometry is missing
                    val center = el.optJSONObject("center")
                    if (center != null) {
                        val wLat = center.getDouble("lat")
                        val wLon = center.getDouble("lon")
                        minDistForWay = haversineDistanceMeters(latitude, longitude, wLat, wLon)
                    }
                }

                // If even the closest point on this way is outside radius, ignore it
                if (minDistForWay > radiusMeters) continue

                val tags = el.optJSONObject("tags")
                val name = tags?.optString("name")?.takeIf { it.isNotBlank() }
                val waterway = tags?.optString("waterway")?.takeIf { it.isNotBlank() }
                val natural = tags?.optString("natural")?.takeIf { it.isNotBlank() }
                val landuse = tags?.optString("landuse")?.takeIf { it.isNotBlank() }
                val water = tags?.optString("water")?.takeIf { it.isNotBlank() }

                // 2) Compute "priority" for this waterbody
                val score = computeWaterPriority(
                    name = name,
                    waterway = waterway,
                    natural = natural,
                    landuse = landuse,
                    water = water
                )

                // 3) Choose best by (score, then distance)
                if (score > bestScore || (score == bestScore && minDistForWay < bestDistance)) {
                    bestScore = score
                    bestDistance = minDistForWay
                    bestElement = el
                }
            }

            if (bestElement == null) {
                Log.d(TAG, "No suitable water element found within radius")
                return@withContext null
            }

            val tags = bestElement.optJSONObject("tags")
            val name = tags?.optString("name")?.takeIf { it.isNotBlank() }
            val type =
                tags?.optString("waterway")?.takeIf { it.isNotBlank() }
                    ?: tags?.optString("natural")?.takeIf { it.isNotBlank() }
                    ?: tags?.optString("landuse")?.takeIf { it.isNotBlank() }
                    ?: tags?.optString("water")?.takeIf { it.isNotBlank() }

            Log.d(
                TAG,
                "Chosen water WITHIN $radiusMeters m: name=$name type=$type dist=$bestDistance score=$bestScore"
            )

            return@withContext NearbyWaterBody(
                name = name,
                type = type,
                distanceMeters = bestDistance
            )
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error querying Overpass", e)
        return@withContext null
    }
}

// Score the "niceness" of a waterbody so we prefer real rivers/lakes with names
private fun computeWaterPriority(
    name: String?,
    waterway: String?,
    natural: String?,
    landuse: String?,
    water: String?
): Int {
    var score = 0

    // Big rivers / streams first
    if (waterway == "river" || water == "river") {
        score += 120
    } else if (waterway == "stream" || water == "stream") {
        score += 110
    }

    // Lakes / reservoirs / general water surfaces
    if (natural == "water" ||
        landuse == "reservoir" ||
        water in listOf("lake", "pond", "reservoir")
    ) {
        score += 100
    }

    // Canals okay-ish
    if (waterway == "canal") {
        score += 60
    }

    // Drains / ditches are least interesting
    if (waterway in listOf("drain", "ditch", "drainage", "drainage_channel")) {
        score += 10
    }

    // Named features are almost always better than unnamed junk
    if (!name.isNullOrBlank()) {
        score += 40
    }

    return score
}

// ----------------------
// Distance helper
// ----------------------

private fun haversineDistanceMeters(
    lat1: Double,
    lon1: Double,
    lat2: Double,
    lon2: Double
): Double {
    val R = 6371000.0 // Earth radius in meters
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2).pow(2.0) +
            cos(Math.toRadians(lat1)) *
            cos(Math.toRadians(lat2)) *
            sin(dLon / 2).pow(2.0)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return R * c
}
