package hs.flensburg.marlin.plugins

import de.lambda9.tailwind.core.KIO
import hs.flensburg.marlin.Config
import hs.flensburg.marlin.business.api.admin.boundary.configureAdmin
import hs.flensburg.marlin.business.api.auth.boundary.configureAuth
import hs.flensburg.marlin.business.api.location.boundary.configureLocation
import hs.flensburg.marlin.business.api.potentialSensors.boundary.configurePotentialSensors
import hs.flensburg.marlin.business.api.sensors.boundary.configureSensors
import hs.flensburg.marlin.business.api.userDevice.boundary.configureUserDevices
import hs.flensburg.marlin.business.api.users.boundary.configureUsers
import io.github.smiley4.ktoropenapi.OpenApi
import io.github.smiley4.ktoropenapi.config.AuthScheme
import io.github.smiley4.ktoropenapi.config.AuthType
import io.github.smiley4.ktoropenapi.get
import io.github.smiley4.ktoropenapi.openApi
import io.github.smiley4.ktorswaggerui.swaggerUI
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.authenticate
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.UnsupportedMediaTypeException
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
    configureAdmin()
    configurePotentialSensors()
    configureLocation()
    configureUserDevices()

    install(XForwardedHeaders)
    install(ForwardedHeaders)
    install(OpenApi) {
        server {
            url = config.backendUrl
        }

        security {
            securityScheme("BearerAuth") {
                type = AuthType.HTTP
                scheme = AuthScheme.BEARER
                bearerFormat = "JWT"
                description = "JWT access token for authenticated users. Obtain via /login, /register, /login/google/android, or /magic-link/login endpoints. Token expires after 15 minutes - use /auth/refresh to obtain a new token pair."
            }

            securityScheme("BearerAuthAdmin") {
                type = AuthType.HTTP
                scheme = AuthScheme.BEARER
                bearerFormat = "JWT"
                description = "JWT access token with admin role. Only users with 'ADMIN' role in their JWT claims can access admin endpoints. Obtain via login endpoints if user has admin privileges."
            }

            securityScheme("OAuth2Google") {
                type = AuthType.OAUTH2
                flows {
                    authorizationCode {
                        authorizationUrl = "https://accounts.google.com/o/oauth2/v2/auth"
                        tokenUrl = "https://oauth2.googleapis.com/token"
                        scopes = mapOf(
                            "openid" to "OpenID Connect authentication",
                            "email" to "Access user's email address",
                            "profile" to "Access user's basic profile information"
                        )
                    }
                }
                description = "Google OAuth2 authentication flow. Redirects to Google for authentication, then returns JWT tokens via callback."
            }
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
        exception<UnsupportedMediaTypeException> { call, cause ->
            call.respond(
                HttpStatusCode.UnsupportedMediaType,
                mapOf("error" to (cause.cause?.message ?: "Invalid Media Type"))
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
    ADMIN("admin"),
    HARBOUR_CONTROL("harbor_control");

    override fun toString(): String = value
}