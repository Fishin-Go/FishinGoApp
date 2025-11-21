//package com.fishingo.backend.model
//
//import org.jetbrains.exposed.sql.ReferenceOption
//import org.jetbrains.exposed.sql.Table
//import org.jetbrains.exposed.sql.javatime.datetime
//import java.time.LocalDateTime
//
//
//// ----------------------------
//// Exposed Table (matches SQL)
//// ----------------------------
//object FishCatchTable : Table("fish_catches") {
//
//    val id = integer("id").autoIncrement()          // SERIAL PRIMARY KEY
//    val userId = integer("user_id")
//        .references(UserTable.id, onDelete = ReferenceOption.CASCADE)
//
//    val fishName = varchar("fish_name", 100)
//    val region = varchar("region", 50)
//    val locationName = varchar("location_name", 100).nullable()
//
//    val latitude = double("latitude").nullable()
//    val longitude = double("longitude").nullable()
//
//    val description = text("description").nullable()   // NEW COLUMN
//
//    val dateCaught = datetime("date_caught")           // TIMESTAMP DEFAULT NOW()
//
//    override val primaryKey = PrimaryKey(id)
//}
//
//
//// ----------------------------
//// Kotlin “Entity” for backend
//// Represents one row in fish_catches
//// ----------------------------
//data class FishCatchEntity(
//    val id: Int,
//    val userId: Int,
//    val fishName: String,
//    val region: String,
//    val locationName: String?,
//    val latitude: Double?,
//    val longitude: Double?,
//    val description: String?,       // NEW
//    val dateCaught: LocalDateTime
//)