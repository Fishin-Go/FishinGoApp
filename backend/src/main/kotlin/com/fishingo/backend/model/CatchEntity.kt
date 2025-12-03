package com.fishingo.backend.model

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

// --------------------------------------------------
// Exposed Table mapping the SQL table "fish_catches"
// --------------------------------------------------
object FishCatchTable : Table("fish_catches") {

    val id = integer("id").autoIncrement()
    val userId = integer("user_id") // FK enforced in database
    val fishName = varchar("fish_name", 100)
    val region = varchar("region", 100)

    val locationName = varchar("location_name", 200).nullable()
    val latitude = double("latitude").nullable()
    val longitude = double("longitude").nullable()

    val dateCaught = datetime("date_caught")  // TIMESTAMP column
    val description = text("description").nullable()

    override val primaryKey = PrimaryKey(id)
}

// --------------------------------------------------
// Kotlin entity representing a row from fish_catches
// --------------------------------------------------
data class CatchEntity(
    val id: Int,
    val userId: Int,
    val fishName: String,
    val region: String,
    val locationName: String?,
    val latitude: Double?,
    val longitude: Double?,
    val dateCaught: LocalDateTime,
    val description: String?
)
