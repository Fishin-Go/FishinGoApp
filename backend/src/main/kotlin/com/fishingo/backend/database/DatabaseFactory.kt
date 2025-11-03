package com.fishingo.backend.database

import com.fishingo.backend.model.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {
    fun init() {
        Database.connect(
            url = "jdbc:postgresql://localhost:5432/fishingo",
            driver = "org.postgresql.Driver",
            user = "postgres",
            password = "postgres"   // <-- set yours
        )
        // create the tables if missing
        transaction {
            SchemaUtils.create(UserTable, WaterBodyTable, FishTable, CatchTable)
        }
        println("DB connected & schema ensured.")
    }
}
