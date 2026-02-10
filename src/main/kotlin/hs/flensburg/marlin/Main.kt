package hs.flensburg.marlin

import hs.flensburg.marlin.Config.Companion.parseConfig
import hs.flensburg.marlin.business.Env
import hs.flensburg.marlin.business.JEnv
import hs.flensburg.marlin.business.configureScheduling
import hs.flensburg.marlin.plugins.configureCORS
import hs.flensburg.marlin.plugins.configureKIO
import hs.flensburg.marlin.plugins.configureRouting
import hs.flensburg.marlin.plugins.configureSerialization
import com.stripe.Stripe
import io.github.cdimascio.dotenv.dotenv
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.application.Application
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import org.flywaydb.core.Flyway

private val logger = KotlinLogging.logger { }

fun main(args: Array<String>) {
    val mode = System.getenv("MODE")?.uppercase() ?: "DEV"

    logger.info { "Starting Marlin-Backend in $mode mode" }

    val config = when (mode) {
        "DEV" -> dotenv()
        else -> null
    }

    val appConfig = config?.parseConfig() ?: Config.parseConfig()
    val (env, dsl) = Env.configure(appConfig)

    Stripe.apiKey = appConfig.stripe.secretKey

    Flyway(
        Flyway.configure()
            .driver("org.postgresql.Driver")
            .dataSource(dsl)
            .locations("classpath:db/migration", "classpath:db/views", "classpath:db/functions")
            .schemas("marlin")
    ).migrate()

    embeddedServer(
        factory = Netty,
        port = env.env.config.http.port,
        host = env.env.config.http.host,
        module = { modules(env) }
    ).start(wait = true)
}

fun Application.modules(env: JEnv) {
    configureSerialization()
    configureKIO(env)
    configureScheduling(env)
    configureCORS()
    configureRouting(env.env.config)
}
