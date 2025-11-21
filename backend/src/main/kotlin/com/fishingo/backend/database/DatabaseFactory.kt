package com.fishingo.backend.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database

object DatabaseFactory {

    fun init() {
        val dataSource = createHikariDataSource()
        Database.connect(dataSource)
    }

    private fun createHikariDataSource(): HikariDataSource {
        val config = HikariConfig().apply {
            jdbcUrl = "jdbc:postgresql://fishingo-db-fishingoapp.c.aivencloud.com:14892/defaultdb?sslmode=require"
            username = "avnadmin"
            password = "your-aiven-password"   // <- your password

            driverClassName = "org.postgresql.Driver"

            maximumPoolSize = 5
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            validate()
        }

        return HikariDataSource(config)
    }
}
