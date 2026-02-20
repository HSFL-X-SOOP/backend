package hs.flensburg.marlin.core

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import de.lambda9.tailwind.core.Cause
import de.lambda9.tailwind.core.KIO
import de.lambda9.tailwind.core.KIO.Companion.unsafeRunSync
import de.lambda9.tailwind.core.KIOException
import de.lambda9.tailwind.core.extensions.exit.fold
import de.lambda9.tailwind.jooq.JIO
import de.lambda9.tailwind.jooq.Jooq
import hs.flensburg.marlin.Config
import hs.flensburg.marlin.business.Env
import hs.flensburg.marlin.business.JEnv
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
        protected lateinit var jooqEnv: Jooq<Env>

        @JvmStatic
        @BeforeAll
        fun setupDatabase() {
            val hikariConfig = HikariConfig().apply {
                jdbcUrl = System.getProperty("test.database.url", "jdbc:postgresql://localhost:6001/marlin_test")
                username = System.getProperty("test.database.user", "user")
                password = System.getProperty("test.database.password", "sql")
                driverClassName = "org.postgresql.Driver"
                maximumPoolSize = 5
                minimumIdle = 1
                connectionTimeout = 10000
                idleTimeout = 30000
            }

            dataSource = HikariDataSource(hikariConfig)
            dsl = DSL.using(dataSource, SQLDialect.POSTGRES)

            val testEnv = createTestEnv()
            val jooqConfig = createJooqConfig()

            val (jooq, _) = Jooq.create(env = testEnv, config = jooqConfig)
            jooqEnv = jooq
        }

        private fun createTestEnv(): Env = Env(
            config = Config(
                mode = Config.Mode.DEV,
                http = Config.Http(
                    host = "localhost",
                    port = 8080,
                ),
                database = Config.Database(
                    url = System.getProperty("test.database.url", "jdbc:postgresql://localhost:6001/marlin_test"),
                    user = System.getProperty("test.database.user", "user"),
                    password = System.getProperty("test.database.password", "sql"),
                ),
                mail = Config.Mail(
                    host = "localhost",
                    port = 587,
                    username = "test@example.com",
                    password = "testpassword",
                    sendFrom = "test@example.com"
                ),
                auth = Config.Auth(
                    jwtSecret = "test-secret-key-for-testing-only",
                    jwtIssuer = "test-issuer",
                    jwtAudience = "test-audience"
                ),
                googleAuth = Config.GoogleAuth(
                    clientId = "test-google-client-id",
                    clientSecret = "test-google-client-secret",
                    iosClientId = "test-ios-client-id"
                ),
                appleAuth = Config.AppleAuth(
                    clientId = "test-apple-client-id"
                ),
                ipInfo = Config.IPInfo(
                    token = "test-ipinfo-token"
                ),
                firebaseInfo = Config.FirebaseInfo(
                    firebaseServiceAccountKeyPath = "/test/path",
                    firebaseCloudMessagingProjectID = "test-project-id"
                ),
                stripe = Config.Stripe(
                    secretKey = "test-stripe-api-key",
                    publishableKey = "test-stripe-publishable-key",
                    webhookSecret = "test-stripe-webhook-secret",
                    notificationPriceId = "test-notification-price-id",
                    apiAccessPriceId = "test-api-access-price-id",
                    trialDays = 14
                ),
                dataSources = Config.DataSources(
                    FrostServerPath = "test-frost-server-path"
                )
            )
        )

        private fun createJooqConfig(): Jooq.Config = Jooq.Config(
            url = System.getProperty("test.database.url", "jdbc:postgresql://localhost:6001/marlin_test"),
            user = System.getProperty("test.database.user", "user"),
            password = System.getProperty("test.database.password", "sql"),
            schema = "marlin"
        )

        @JvmStatic
        @AfterAll
        fun teardownDatabase() {
            if (::dataSource.isInitialized && dataSource is HikariDataSource) {
                (dataSource as HikariDataSource).close()
            }
        }
    }

    @JvmName("JIOrun")
    protected operator fun <A> JIO<A>.not(): A {
        val exit = unsafeRunSync(jooqEnv)
        return exit.fold(
            onDefect = { throw it },
            onError = { if (it is Throwable) throw it else throw KIOException(Cause.expected(it)) },
            onSuccess = { it },
        )
    }

    @JvmName("KIOrun")
    protected operator fun <E, A> KIO<JEnv, E, A>.not(): A {
        val exit = unsafeRunSync(jooqEnv)
        return exit.fold(
            onDefect = { throw it },
            onError = { if (it is Throwable) throw it else throw KIOException(Cause.expected(it)) },
            onSuccess = { it },
        )
    }
}
