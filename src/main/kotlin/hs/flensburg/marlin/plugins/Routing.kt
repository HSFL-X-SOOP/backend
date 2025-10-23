package hs.flensburg.marlin.plugins

import de.lambda9.tailwind.core.KIO
import hs.flensburg.marlin.Config
import hs.flensburg.marlin.business.api.auth.boundary.configureAuth
import hs.flensburg.marlin.business.api.location.boundary.configureLocation
import hs.flensburg.marlin.business.api.potentialSensors.boundary.configurePotentialSensors
import hs.flensburg.marlin.business.api.sensors.boundary.configureSensors
import hs.flensburg.marlin.business.api.users.boundary.configureUsers
import io.github.smiley4.ktoropenapi.OpenApi
import io.github.smiley4.ktoropenapi.get
import io.github.smiley4.ktoropenapi.openApi
import io.github.smiley4.ktorswaggerui.swaggerUI
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.authenticate
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.forwardedheaders.ForwardedHeaders
import io.ktor.server.plugins.forwardedheaders.XForwardedHeaders
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.Route
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.serialization.SerializationException


fun Application.configureRouting(config: Config) {
    configureAuth(config)
    configureUsers()
    configureSensors()
    configurePotentialSensors()
    configureLocation()

    install(XForwardedHeaders)
    install(ForwardedHeaders)
    install(OpenApi) {
        server {
            url = config.backendUrl
        }
    }

    install(StatusPages) {
        exception<BadRequestException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to (cause.cause?.message ?: "Invalid request data"))
            )
        }

        exception<SerializationException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "Malformed JSON: ${cause.cause?.message}")
            )
        }
    }

    routing {
        get(path = "/health", builder = { description = "Health check endpoint" }) {
            call.respondKIO(KIO.ok("Marlin-Backend is running!"))
        }

        route("/api.json") { openApi() }

        if (config.mode == Config.Mode.PROD || config.mode == Config.Mode.STAGING) {
            route("/swagger") { swaggerUI("/api/api.json") }
            get({ hidden = true }) { call.respondRedirect("/api/swagger", permanent = false) }
        } else {
            route("/swagger") { swaggerUI("/api.json") }
            get({ hidden = true }) { call.respondRedirect("/swagger", permanent = false) }
        }
    }
}

fun Route.authenticate(realm: Realm, block: Route.() -> Unit) {
    authenticate(realm.value) {
        block()
    }
}

enum class Realm(val value: String) {
    COMMON("common"),
    ADMIN("admin");

    override fun toString(): String = value
}