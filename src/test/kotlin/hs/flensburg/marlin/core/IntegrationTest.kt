package hs.flensburg.marlin.core

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.impl.DSL
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import javax.sql.DataSource

abstract class IntegrationTest {

    companion object {
        private lateinit var dataSource: DataSource

        @JvmStatic
        protected lateinit var dsl: DSLContext

        @JvmStatic
        @BeforeAll
        fun setupDatabase() {
            val config = HikariConfig().apply {
                jdbcUrl = System.getProperty("test.database.url", "jdbc:postgresql://localhost:6001/marlin_test")
                username = System.getProperty("test.database.user", "user")
                password = System.getProperty("test.database.password", "sql")
                driverClassName = "org.postgresql.Driver"
                maximumPoolSize = 5
                minimumIdle = 1
                connectionTimeout = 10000
                idleTimeout = 30000
            }

            dataSource = HikariDataSource(config)
            dsl = DSL.using(dataSource, SQLDialect.POSTGRES)
        }

        @JvmStatic
        @AfterAll
        fun teardownDatabase() {
            if (::dataSource.isInitialized && dataSource is HikariDataSource) {
                (dataSource as HikariDataSource).close()
            }
        }
    }
}
