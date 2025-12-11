package com.fishingo

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import kotlin.math.*

object WaterDatabaseManager {

    private const val TAG = "WaterDB"

    // DB file name in assets/ and internal /databases/
    private const val DB_NAME = "water_skeleton.sqlite"
    private const val ASSET_NAME = "water_skeleton.sqlite"

    // TABLE + COLUMNS (based on your screenshot)
    private const val TABLE_NAME = "water_skeleton"   // change this if your table is named differently
    private const val COL_X = "xcoord"   // longitude
    private const val COL_Y = "ycoord"   // latitude
    private const val COL_NAME = "name"
    private const val COL_TYPE = "type"

    private const val DEBUG_FILE = "water_debug.txt"

    private var initialized = false
    private lateinit var appContext: Context
    private lateinit var db: SQLiteDatabase

    data class WaterHit(
        val latitude: Double,
        val longitude: Double,
        val name: String?,
        val type: String?,
        val distanceMeters: Double
    )

    /**
     * Call once from MainActivity.onCreate(applicationContext).
     * - copies the DB from assets to /databases if not present
     * - opens it read-only
     * - writes info to water_debug.txt
     */
    fun initialize(context: Context) {
        if (initialized) return

        appContext = context.applicationContext
        clearDebugLog()
        log("===== WaterDatabaseManager initialize() =====")

        val dbFile = appContext.getDatabasePath(DB_NAME)
        if (!dbFile.exists() || dbFile.length() == 0L) {
            dbFile.parentFile?.mkdirs()
            log("Copying $ASSET_NAME from assets to ${dbFile.absolutePath}")
            copyAssetToFile(ASSET_NAME, dbFile)
        } else {
            log("DB already present, skipping copy. size=${dbFile.length()} bytes")
        }

        log("Opening DB at: ${dbFile.absolutePath} (exists=${dbFile.exists()}, size=${dbFile.length()} bytes)")
        db = SQLiteDatabase.openDatabase(
            dbFile.absolutePath,
            null,
            SQLiteDatabase.OPEN_READONLY
        )

        // sanity check: row count
        try {
            db.rawQuery("SELECT COUNT(*) FROM $TABLE_NAME", null).use { c ->
                val count = if (c.moveToFirst()) c.getLong(0) else -1L
                log("DB opened. Table '$TABLE_NAME' row count = $count")
            }
        } catch (e: Exception) {
            log("ERROR counting rows in $TABLE_NAME: ${e.message}")
        }
// EXTRA: log coordinate ranges so we see what units they're in
        try {
            db.rawQuery(
                "SELECT MIN($COL_X), MAX($COL_X), MIN($COL_Y), MAX($COL_Y) FROM $TABLE_NAME",
                null
            ).use { c ->
                if (c.moveToFirst()) {
                    val minX = c.getDouble(0)
                    val maxX = c.getDouble(1)
                    val minY = c.getDouble(2)
                    val maxY = c.getDouble(3)
                    log("Coord ranges: $COL_X in [$minX .. $maxX], $COL_Y in [$minY .. $maxY]")
                }
            }
        } catch (e: Exception) {
            log("ERROR reading coord ranges: ${e.message}")
        }
        initialized = true
    }

    /**
     * Find nearest water within [radiusMeters] of (latitude, longitude).
     * Returns null if no point within radius.
     */
    fun findNearestWater(
        latitude: Double,
        longitude: Double,
        radiusMeters: Double
    ): WaterHit? {
        if (!initialized) {
            log("findNearestWater called but DB not initialized")
            return null
        }

        log("---- findNearestWater lat=$latitude, lon=$longitude, radius=$radiusMeters ----")

        // Rough meter → degree conversion
        val latRad = Math.toRadians(latitude)
        val degLatPerMeter = 1.0 / 111000.0
        val degLonPerMeter = 1.0 / (111000.0 * cos(latRad))

// We search in at least 300m radius to be safe,
// but we STILL enforce radiusMeters at the end.
        val searchRadius = maxOf(radiusMeters, 300.0)

        val deltaLat = searchRadius * degLatPerMeter
        val deltaLon = searchRadius * degLonPerMeter

        val minLat = latitude - deltaLat
        val maxLat = latitude + deltaLat
        val minLon = longitude - deltaLon
        val maxLon = longitude + deltaLon

        log("bbox (searchRadius=$searchRadius): lat [$minLat .. $maxLat], lon [$minLon .. $maxLon]")

        log("bbox: lat [$minLat .. $maxLat], lon [$minLon .. $maxLon]")

        // IMPORTANT: xcoord = lon, ycoord = lat
        val cursor = db.rawQuery(
            """
            SELECT $COL_X, $COL_Y, $COL_NAME, $COL_TYPE
            FROM $TABLE_NAME
            WHERE $COL_X BETWEEN ? AND ?
              AND $COL_Y BETWEEN ? AND ?
            """.trimIndent(),
            arrayOf(
                minLon.toString(),  // xcoord = lon
                maxLon.toString(),
                minLat.toString(),  // ycoord = lat
                maxLat.toString()
            )
        )

        cursor.use {
            log("SQL executed, cursor.count = ${it.count}")

            var best: WaterHit? = null
            var bestDist = Double.MAX_VALUE
            var candidateCount = 0

            val idxX = it.getColumnIndexOrThrow(COL_X)
            val idxY = it.getColumnIndexOrThrow(COL_Y)
            val idxName = it.getColumnIndexOrThrow(COL_NAME)
            val idxType = it.getColumnIndexOrThrow(COL_TYPE)

            while (it.moveToNext()) {
                candidateCount++

                val lon = it.getDouble(idxX)
                val lat = it.getDouble(idxY)
                val name = if (!it.isNull(idxName)) it.getString(idxName) else null
                val type = if (!it.isNull(idxType)) it.getString(idxType) else null

                val dist = haversineMeters(latitude, longitude, lat, lon)

                if (dist < bestDist) {
                    bestDist = dist
                    best = WaterHit(
                        latitude = lat,
                        longitude = lon,
                        name = name,
                        type = type,
                        distanceMeters = dist
                    )
                }
            }

            log("candidateCount = $candidateCount")
            if (best != null) {
                log("bestHit distance = $bestDist m, name=${best.name}, type=${best.type}")
            } else {
                log("bestHit distance = none")
            }

            return if (best != null && bestDist <= radiusMeters) {
                log("RETURN water within radius")
                best
            } else {
                log("RETURN no water within radius")
                null
            }
        }
    }

    // ----------------- helpers -----------------

    private fun copyAssetToFile(assetName: String, outFile: File) {
        try {
            appContext.assets.open(assetName).use { input ->
                FileOutputStream(outFile).use { output ->
                    input.copyTo(output)
                }
            }
        } catch (e: Exception) {
            log("ERROR copying asset $assetName: ${e.message}")
        }
    }

    private fun haversineMeters(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val r = 6_371_000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2.0) +
                cos(Math.toRadians(lat1)) *
                cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    private fun clearDebugLog() {
        val ctx = if (::appContext.isInitialized) appContext else return
        val dir = ctx.getExternalFilesDir(null) ?: return
        val f = File(dir, DEBUG_FILE)
        if (f.exists()) f.delete()
    }

    private fun log(msg: String) {
        Log.d(TAG, msg)
        val ctx = if (::appContext.isInitialized) appContext else return
        try {
            val dir = ctx.getExternalFilesDir(null) ?: return
            val f = File(dir, DEBUG_FILE)
            f.appendText(msg + "\n")
        } catch (_: Exception) {
        }
    }
}
