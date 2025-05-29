package hs.flensburg.soop

import hs.flensburg.soop.Config.Companion.parseConfig
import hs.flensburg.soop.business.Env
import hs.flensburg.soop.business.Env.AppEnvironment
import hs.flensburg.soop.plugins.configureSerialization
import io.github.cdimascio.dotenv.dotenv
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.application.Application
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

private val logger = KotlinLogging.logger { }

fun main(args: Array<String>) {
    val envPath = args.getOrNull(0)

//    val config = envPath?.let {
//        dotenv { directory = it }
//    } ?: dotenv()
//
//    val appEnv = Env.configure(config.parseConfig())

    embeddedServer(
        factory = Netty,
        port = 8080,
        host = "0.0.0.0",
        module = { modules() }
    ).start(wait = true)
}

fun Application.modules() {
    configureSerialization()

    routing {
        route("/test") {
            get {
                call.respondText("Test route is working!")
            }
        }
    }
}
