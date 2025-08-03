package hs.flensburg.marlin.business.api.auth.boundary

import de.lambda9.tailwind.core.KIO.Companion.unsafeRunSync
import hs.flensburg.marlin.Config
import hs.flensburg.marlin.business.ServiceLayerError
import hs.flensburg.marlin.business.api.auth.control.JWTAuthority
import hs.flensburg.marlin.business.api.auth.entity.LoggedInUser
import hs.flensburg.marlin.business.api.auth.entity.LoginRequest
import hs.flensburg.marlin.business.api.auth.entity.LoginResponse
import hs.flensburg.marlin.business.api.auth.entity.MagicLinkLoginRequest
import hs.flensburg.marlin.business.api.auth.entity.MagicLinkRequest
import hs.flensburg.marlin.business.api.auth.entity.RefreshTokenRequest
import hs.flensburg.marlin.business.api.auth.entity.RegisterRequest
import hs.flensburg.marlin.business.api.auth.entity.VerifyEmailRequest
import hs.flensburg.marlin.business.api.email.boundary.EmailService
import hs.flensburg.marlin.plugins.Realm
import hs.flensburg.marlin.plugins.authenticate
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
import io.ktor.server.plugins.origin
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
            urlProvider = {
                if (envConfig.mode == Config.Mode.PROD) {
                    "https://marlin-live.com/api/auth/google/callback"
                } else {
                    "http://localhost:8080/auth/google/callback"
                }
            }

            providerLookup = {
                OAuthServerSettings.OAuth2ServerSettings(
                    name = "google",
                    authorizeUrl = "https://accounts.google.com/o/oauth2/v2/auth",
                    accessTokenUrl = "https://oauth2.googleapis.com/token",
                    requestMethod = HttpMethod.Post,
                    clientId = envConfig.googleAuth.clientId,
                    clientSecret = envConfig.googleAuth.clientSecret,
                    defaultScopes = listOf("openid", "email", "profile"),
                    extraAuthParameters = listOf(
                        "access_type" to "offline",
                        "prompt" to "consent"
                    ),
                )
            }

            client = HttpClient(CIO)
        }
    }

    routing {
        authenticate("auth-oauth-google") {
            get(
                path = "/login/google",
                builder = {
                    tags("auth")
                    description = "Redirect to Google for OAuth login"
                }) {}

            get("/auth/google/callback", { hidden = true }) {

                val token = call.principal<OAuthAccessTokenResponse.OAuth2>() ?: error("No OAuth2 principal")

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
                    HttpStatusCode.Unauthorized to {
                        body<String>()
                    }
                    HttpStatusCode.TooManyRequests to {
                        body<String>()
                    }
                }
            }
        ) {
            val loginRequest = call.receive<LoginRequest>()
            val clientIp = call.request.origin.remoteAddress
            val env = call.kioEnv

            AuthService.login(loginRequest, clientIp).unsafeRunSync(env).fold(
                onSuccess = { it },
                onError = { error ->
                    val e = error.failures().first()
                    when (e) {
                        is AuthService.Error.OAuthRedirectRequired -> {
                            val redirectUrl = if (envConfig.mode == Config.Mode.PROD) {
                                "/api/login/google"
                            } else {
                                "/login/google"
                            }
                            call.respondRedirect(redirectUrl, permanent = false)
                        }

                        else -> e.toApiError().let { call.respond(it.statusCode, it.message) }
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
                    body<RefreshTokenRequest>()
                }
                response {
                    HttpStatusCode.OK to {
                        body<LoginResponse>()
                    }
                }
            }
        ) {
            val refreshToken = call.receive<RefreshTokenRequest>()

            call.respondKIO(AuthService.refreshToken(refreshToken))
        }

        post(
            path = "/magic-link",
            builder = {
                description = "Request a magic link for a passwordless login"
                tags("auth")
                request {
                    body<MagicLinkRequest>()
                }
                response {
                    HttpStatusCode.OK to {
                        body<Unit>()
                    }
                }
            }
        ) {
            val magicLinkRequest = call.receive<MagicLinkRequest>()

            call.respondKIO(EmailService.sendMagicLinkEmail(magicLinkRequest.email))
        }

        post(
            path = "/magic-link/login",
            builder = {
                description = "Login using a magic link"
                tags("auth")
                request {
                    body<MagicLinkLoginRequest>()
                }
                response {
                    HttpStatusCode.OK to {
                        body<LoginResponse>()
                    }
                    HttpStatusCode.Unauthorized to {
                        body<String>()
                    }
                }
            }
        ) {
            val magicLinkLoginRequest = call.receive<MagicLinkLoginRequest>()

            call.respondKIO(AuthService.loginViaMagicLink(magicLinkLoginRequest))
        }

        post(
            path = "/verify-email",
            builder = {
                description = "Verify the user's email address"
                tags("auth")
                request {
                    body<VerifyEmailRequest>()
                }
                response {
                    HttpStatusCode.OK to {
                        body<Unit>()
                    }
                    HttpStatusCode.BadRequest to {
                        body<String>()
                    }
                }
            }
        ) {
            val verifyEmailRequest = call.receive<VerifyEmailRequest>()

            call.respondKIO(AuthService.verifyEmail(verifyEmailRequest))
        }

        authenticate(Realm.COMMON) {
            post(
                path = "/send-verification-email",
                builder = {
                    description = "Send a verification email to the authenticated user"
                    tags("auth")
                    response {
                        HttpStatusCode.OK to {
                            body<Unit>()
                        }
                    }
                }
            ) {
                val user = call.principal<LoggedInUser>()!!

                call.respondKIO(EmailService.sendVerificationEmail(user.id))
            }
        }
    }
}