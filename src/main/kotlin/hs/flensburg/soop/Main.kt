package hs.flensburg.soop

import hs.flensburg.soop.Config.Companion.parseConfig
import hs.flensburg.soop.business.Env
import hs.flensburg.soop.business.Env.AppEnvironment
import hs.flensburg.soop.business.Result
import hs.flensburg.soop.plugins.configureSerialization
import hs.flensburg.soop.plugins.respondResult
import io.github.cdimascio.dotenv.dotenv
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import org.flywaydb.core.Flyway
import org.slf4j.event.Level

private val logger = KotlinLogging.logger { }

fun main(args: Array<String>) {
    val envPath = args.getOrNull(0)

    val config = envPath?.let {
        dotenv { directory = it }
    } ?: dotenv()

    val appEnv = Env.configure(config.parseConfig())

    Flyway(
        Flyway.configure()
            .driver("org.postgresql.Driver")
            .dataSource(appEnv.dataSource)
            .schemas("soop")
    ).migrate()

    embeddedServer(
        factory = Netty,
        port = appEnv.config.http.port,
        host = appEnv.config.http.host,
        module = { modules(appEnv) }
    ).start(wait = true)
}

fun Application.modules(env: AppEnvironment) {
    configureSerialization()
}
