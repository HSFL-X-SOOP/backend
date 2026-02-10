package hs.flensburg.marlin.business.api.auth.control

import de.lambda9.tailwind.core.KIO.Companion.unsafeRunSync
import hs.flensburg.marlin.business.api.apikey.boundary.ApiKeyService
import hs.flensburg.marlin.plugins.kioEnv
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.AuthenticationConfig
import io.ktor.server.auth.AuthenticationContext
import io.ktor.server.auth.AuthenticationFailedCause
import io.ktor.server.auth.AuthenticationProvider
import io.ktor.server.response.respond

class ApiKeyAuthProvider internal constructor(
    private val providerName: String
) : AuthenticationProvider(object : Config(providerName) {}) {

    override suspend fun onAuthenticate(context: AuthenticationContext) {
        val call = context.call
        val apiKey = call.request.headers["X-API-Key"]

        if (apiKey.isNullOrBlank()) {
            context.challenge("ApiKeyAuth", AuthenticationFailedCause.NoCredentials) { challenge, c ->
                c.respond(HttpStatusCode.Unauthorized, "Missing X-API-Key header")
                challenge.complete()
            }
            return
        }

        val env = call.kioEnv
        val result = ApiKeyService.validateApiKey(apiKey).unsafeRunSync(env)

        result.fold(
            onSuccess = { loggedInUser ->
                context.principal(loggedInUser)
            },
            onError = {
                context.challenge("ApiKeyAuth", AuthenticationFailedCause.InvalidCredentials) { challenge, c ->
                    c.respond(HttpStatusCode.Unauthorized, "Invalid API key")
                    challenge.complete()
                }
            }
        )
    }
}

fun AuthenticationConfig.apiKey(name: String, configure: () -> Unit = {}) {
    configure()
    register(ApiKeyAuthProvider(name))
}
