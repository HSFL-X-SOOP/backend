package hs.flensburg.marlin.business.api.auth.boundary

import de.lambda9.tailwind.core.KIO.Companion.unsafeRunSync
import hs.flensburg.marlin.Config
import hs.flensburg.marlin.business.ServiceLayerError
import hs.flensburg.marlin.business.api.auth.control.JWTAuthority
import hs.flensburg.marlin.business.api.auth.entity.LoginRequest
import hs.flensburg.marlin.business.api.auth.entity.LoginResponse
import hs.flensburg.marlin.business.api.auth.entity.RefreshRequest
import hs.flensburg.marlin.business.api.auth.entity.RegisterRequest
import hs.flensburg.marlin.plugins.Realm
import hs.flensburg.marlin.plugins.kioEnv
import hs.flensburg.marlin.plugins.respondKIO
import io.github.smiley4.ktoropenapi.post
import io.github.smiley4.ktoropenapi.get
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.OAuthAccessTokenResponse
import io.ktor.server.auth.OAuthServerSettings
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.auth.oauth
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.routing

fun Application.configureAuth(envConfig: Config) {
    JWTAuthority.init(envConfig)

    install(Authentication) {
        jwt(Realm.COMMON.value) {
            realm = "Common-Realm"
            verifier(JWTAuthority.accessVerifier)
            validate { credential ->
                AuthService.validateCommonRealmAccess(credential).unsafeRunSync(kioEnv).fold(
                    onSuccess = { it },
                    onError = { null }
                )
            }
        }

        oauth("auth-oauth-google") {
            urlProvider = { "http://localhost:8080/auth/google/callback" }

            providerLookup = {
                OAuthServerSettings.OAuth2ServerSettings(
                    name = "google",
                    authorizeUrl = "https://accounts.google.com/o/oauth2/v2/auth",
                    accessTokenUrl = "https://oauth2.googleapis.com/token",
                    requestMethod = HttpMethod.Post,
                    clientId = envConfig.googleAuth.clientId,
                    clientSecret = envConfig.googleAuth.clientSecret,
                    defaultScopes = listOf("openid", "email", "profile"),
                    extraAuthParameters = listOf("access_type" to "offline"),
                )
            }

            client = HttpClient(CIO)
        }
    }

    routing {
        authenticate("auth-oauth-google") {
            get("/login/google", { hidden = true }) { }

            get("/auth/google/callback", { hidden = true }) {

                val token = call.principal<OAuthAccessTokenResponse.OAuth2>()
                    ?: error("No OAuth2 principal")

                call.respondKIO(AuthService.loginGoogleUser(token))
            }
        }

        post(
            path = "/register",
            builder = {
                description = "Register a new user"
                tags("auth")
                request {
                    body<RegisterRequest>()
                }
                response {
                    HttpStatusCode.Created to {
                        body<LoginResponse>()
                    }
                }
            }
        ) {
            val registerRequest = call.receive<RegisterRequest>()

            call.respondKIO(AuthService.register(registerRequest))
        }

        post(
            path = "/login",
            builder = {
                description = "Login an existing user"
                tags("auth")
                request {
                    body<LoginRequest>()
                }
                response {
                    HttpStatusCode.OK to {
                        body<LoginResponse>()
                    }
                }
            }
        ) {
            val loginRequest = call.receive<LoginRequest>()

            AuthService.login(loginRequest).unsafeRunSync(call.kioEnv).fold(
                onSuccess = { it },
                onError = { error ->
                    when (error.failures().first()) {
                        is AuthService.Error.OAuthRedirectRequired -> call.respondRedirect(
                            "/login/google",
                            permanent = false
                        )

                        else -> (error as ServiceLayerError).toApiError()
                            .let { gnvError -> call.respond(gnvError.statusCode, gnvError.message) }
                    }
                }
            )
        }

        post(
            path = "/auth/refresh",
            builder = {
                description = "Refresh an access token using a refresh token"
                tags("auth")
                request {
                    body<RefreshRequest>()
                }
                response {
                    HttpStatusCode.OK to {
                        body<LoginResponse>()
                    }
                }
            }
        ) {
            val refreshToken = call.receive<RefreshRequest>()

            call.respondKIO(AuthService.refresh(refreshToken))
        }
    }
}