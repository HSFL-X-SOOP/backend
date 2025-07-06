package hs.flensburg.soop

import de.lambda9.tailwind.core.KIO
import hs.flensburg.soop.Config.Companion.parseConfig
import hs.flensburg.soop.business.Env
import hs.flensburg.soop.business.JEnv
import hs.flensburg.soop.business.configureScheduling
import hs.flensburg.soop.plugins.configureKIO
import hs.flensburg.soop.plugins.configureSerialization
import hs.flensburg.soop.plugins.respondKIO
import io.github.cdimascio.dotenv.dotenv
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.application.Application
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

private val logger = KotlinLogging.logger { }

fun main(args: Array<String>) {
    val mode = System.getenv("MODE")?.uppercase() ?: "DEV"

    val config = when (mode) {
        "STAGING", "DEV" -> dotenv()
        else -> null
    }

    val (env, _) = Env.configure(config?.parseConfig() ?: Config.parseConfig())

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
