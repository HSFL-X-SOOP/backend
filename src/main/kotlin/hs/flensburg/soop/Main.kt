package hs.flensburg.soop

import hs.flensburg.soop.Config.Companion.parseConfig
import hs.flensburg.soop.business.Env
import hs.flensburg.soop.business.JEnv
import hs.flensburg.soop.business.configureScheduling
import hs.flensburg.soop.plugins.configureKIO
import hs.flensburg.soop.plugins.configureSerialization
import io.github.cdimascio.dotenv.dotenv
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.application.Application
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import org.flywaydb.core.Flyway

private val logger = KotlinLogging.logger { }

fun main(args: Array<String>) {
    val mode = System.getenv("MODE")?.uppercase() ?: "DEV"

    val config = when (mode) {
        "STAGING", "DEV" -> dotenv()
        else -> null
    }

    val (env, dsl) = Env.configure(config?.parseConfig() ?: Config.parseConfig())

    Flyway(
        Flyway.configure()
            .driver("org.postgresql.Driver")
            .dataSource(dsl)
            .schemas("soop")
    ).migrate()

    embeddedServer(
        factory = Netty,
        port = 8080,
        host = "0.0.0.0",
        module = { modules(env) }
    ).start(wait = true)
}

fun Application.modules(env: JEnv) {
    configureSerialization()
    configureKIO(env)
    configureScheduling(env)
}
