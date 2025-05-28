package main.kotlin.hs.flensburg.soop

import main.kotlin.hs.flensburg.soop.Config.Companion.parseConfig
import main.kotlin.hs.flensburg.soop.business.Env
import main.kotlin.hs.flensburg.soop.business.Env.AppEnvironment
import main.kotlin.hs.flensburg.soop.plugins.configureSerialization
import io.github.cdimascio.dotenv.dotenv
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.application.Application
import io.ktor.server.engine.*
import io.ktor.server.netty.*

private val logger = KotlinLogging.logger { }

fun main(args: Array<String>) {
    val envPath = args.getOrNull(0)

    val config = envPath?.let {
        dotenv { directory = it }
    } ?: dotenv()

    val appEnv = Env.configure(config.parseConfig())

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
