package org.jetbrains.demo.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import kotlinx.coroutines.DisposableHandle
import kotlinx.serialization.Serializable
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.output.MigrateResult
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager

fun Application.database(config: DatabaseConfig): Database {
    val dataSource = dataSource(config)
    flyway(dataSource, config.flyway)
    val database = Database.connect(dataSource)
    addCloseable {
        TransactionManager.closeAndUnregister(database)
        dataSource.close()
    }
    return database
}

fun Application.addCloseable(closeable: AutoCloseable): DisposableHandle =
    monitor.subscribe(ApplicationStopped) {
        closeable.close()
    }

@Serializable
data class DatabaseConfig(
    val driverClassName: String,
    val host: String,
    val port: Int,
    val name: String,
    val username: String,
    val password: String,
    val maxPoolSize: Int,
    val cachePrepStmts: Boolean,
    val prepStmtCacheSize: Int,
    val prepStmtCacheSqlLimit: Int,
    val flyway: Flyway
) {
    @Serializable
    data class Flyway(val locations: String, val baselineOnMigrate: Boolean)
}


fun flyway(dataSource: HikariDataSource, flywayConfig: DatabaseConfig.Flyway): MigrateResult =
    Flyway.configure()
        .dataSource(dataSource)
        .locations(flywayConfig.locations)
        .baselineOnMigrate(true)
        .load()
        .migrate()

fun dataSource(config: DatabaseConfig): HikariDataSource =
    HikariDataSource(
        HikariConfig().apply {
            jdbcUrl =
                "jdbc:postgresql://${config.host}:${config.port}/${config.name}"
            username = config.username
            password = config.password
            driverClassName = config.driverClassName
            maximumPoolSize = config.maxPoolSize
            addDataSourceProperty("cachePrepStmts", config.cachePrepStmts.toString())
            addDataSourceProperty("prepStmtCacheSize", config.prepStmtCacheSize.toString())
            addDataSourceProperty("prepStmtCacheSqlLimit", config.prepStmtCacheSqlLimit.toString())
        }
    )
