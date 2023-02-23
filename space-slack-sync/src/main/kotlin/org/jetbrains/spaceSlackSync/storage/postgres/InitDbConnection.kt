package org.jetbrains.spaceSlackSync.storage.postgres

import com.zaxxer.hikari.HikariDataSource
import io.ktor.http.*
import io.ktor.server.config.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.spaceSlackSync.config

fun initPostgres(): PostgresStorage? {
    val postgresUrl = config.tryGetString("storage.postgres.url")?.let { Url(it) } ?: return null
    val hikariJdbcUrl =
        "jdbc:postgresql://${postgresUrl.hostWithPort}${postgresUrl.pathSegments.joinToString("/")}?user=${postgresUrl.user}&password=${postgresUrl.password}"

    val dataSource = object : HikariDataSource() {
        init {
            driverClassName = "org.postgresql.Driver"

            jdbcUrl = hikariJdbcUrl
            username = postgresUrl.user!!
            password = postgresUrl.password!!

            maximumPoolSize = 20
            minimumIdle = 2
            connectionTimeout = 60000
            idleTimeout = 90000
            leakDetectionThreshold = 300000

            dataSourceProperties.setProperty("socketTimeout", "30")
            dataSourceProperties.setProperty("tcpKeepAlive", "true")
        }
    }

    val connection = Database.connect(dataSource)

    transaction(connection) {
        SchemaUtils.createMissingTablesAndColumns(*allTables.toTypedArray())
    }

    return PostgresStorage(connection)
}
