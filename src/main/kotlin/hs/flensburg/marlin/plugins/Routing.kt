package hs.flensburg.marlin.plugins

import de.lambda9.tailwind.core.KIO
import hs.flensburg.marlin.business.JEnv
import hs.flensburg.marlin.business.api.auth.boundary.configureAuth
import io.github.smiley4.ktoropenapi.OpenApi
import io.github.smiley4.ktoropenapi.get
import io.github.smiley4.ktoropenapi.openApi
import io.github.smiley4.ktorswaggerui.swaggerUI
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.Route
import io.ktor.server.routing.route
import io.ktor.server.routing.routing


fun Application.configureRouting(env: JEnv) {
    configureAuth(env.env.config)

    install(OpenApi)

    routing {
        get(path = "/health", builder = { description = "Health check endpoint" }) {
            call.respondKIO(KIO.ok("Marlin-Backend is running!"))
        }
        route("/api.json") { openApi() }
        route("/swagger") { swaggerUI("/api.json") }
        get("/") { call.respondRedirect("/swagger", permanent = false) }
    }
}

fun Route.authenticate(realm: Realm, block: Route.() -> Unit) {
    authenticate(realm.value) {
        block()
    }
}

enum class Realm(val value: String) {
    COMMON("common");

    override fun toString(): String = value
}