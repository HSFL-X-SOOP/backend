package hs.flensburg.marlin.business.api.auth.boundary

import de.lambda9.tailwind.core.KIO.Companion.unsafeRunSync
import de.lambda9.tailwind.jooq.transact
import hs.flensburg.marlin.Config
import hs.flensburg.marlin.business.api.auth.control.JWTAuthority
import hs.flensburg.marlin.business.api.auth.entity.AppleLoginRequest
import hs.flensburg.marlin.business.api.auth.entity.AppleNotificationPayload
import hs.flensburg.marlin.business.api.auth.entity.GoogleLoginRequest
import hs.flensburg.marlin.business.api.auth.entity.LoggedInUser
import hs.flensburg.marlin.business.api.auth.entity.LoginRequest
import hs.flensburg.marlin.business.api.auth.entity.LoginResponse
import hs.flensburg.marlin.business.api.auth.entity.MagicLinkLoginRequest
import hs.flensburg.marlin.business.api.auth.entity.MagicLinkRequest
import hs.flensburg.marlin.business.api.auth.entity.RefreshTokenRequest
import hs.flensburg.marlin.business.api.auth.entity.RegisterRequest
import hs.flensburg.marlin.business.api.auth.entity.VerifyEmailRequest
import hs.flensburg.marlin.business.api.email.boundary.EmailService
import hs.flensburg.marlin.business.api.openAPI.AuthOpenAPISpec
import hs.flensburg.marlin.plugins.Realm
import hs.flensburg.marlin.plugins.authenticate
import hs.flensburg.marlin.plugins.kioEnv
import hs.flensburg.marlin.plugins.respondKIO
import io.github.smiley4.ktoropenapi.post
import io.github.smiley4.ktoropenapi.get
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.http.HttpMethod
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
import java.net.URLEncoder

fun Application.configureAuth(envConfig: Config) {
    JWTAuthority.init(envConfig.auth)

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

        jwt(Realm.HARBOUR_CONTROL.value) {
            realm = "Harbor-Control-Realm"
            verifier(JWTAuthority.accessVerifier)
            validate { credential ->
                AuthService.validateHarborControlRealmAccess(credential).unsafeRunSync(kioEnv).fold(
                    onSuccess = { it },
                    onError = { null }
                )
            }
        }

        jwt(Realm.ADMIN.value) {
            realm = "ADMIN-Realm"
            verifier(JWTAuthority.accessVerifier)
            validate { credential ->
                AuthService.validateAdminRealmAccess(credential).unsafeRunSync(kioEnv).fold(
                    onSuccess = { it },
                    onError = { null }
                )
            }
        }

        oauth("auth-oauth-google") {
            urlProvider = { "${envConfig.backendUrl}/auth/google/callback" }

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
            get("/login/google", AuthOpenAPISpec.loginGoogle) {}

            get("/auth/google/callback", { hidden = true }) {

                val token = call.principal<OAuthAccessTokenResponse.OAuth2>() ?: error("No OAuth2 principal")

                val res: LoginResponse = AuthService
                    .loginGoogleUser(token)
                    .unsafeRunSync(call.kioEnv)
                    .fold(
                        onSuccess = { it },
                        onError = { error ->
                            val e = error.failures().first().toApiError()
                            call.respond(e.statusCode, e.message)
                            return@get
                        }
                    )

                val callbackUrl = "${envConfig.frontendUrl}/oauth-callback"

                val redirectUrl = buildString {
                    append(callbackUrl)
                    append("#access_token=")
                    append(URLEncoder.encode(res.accessToken, Charsets.UTF_8))
                    append("&refresh_token=")
                    append(URLEncoder.encode(res.refreshToken, Charsets.UTF_8))
                }

                call.respondRedirect(redirectUrl, permanent = false)
            }
        }

        post("/register", AuthOpenAPISpec.register) {
            val registerRequest = call.receive<RegisterRequest>()

            call.respondKIO(AuthService.register(registerRequest).transact())
        }

        post("/login", AuthOpenAPISpec.login) {
            val loginRequest = call.receive<LoginRequest>()
            val clientIp = call.request.origin.remoteAddress
            val env = call.kioEnv

            AuthService.login(loginRequest, clientIp).unsafeRunSync(env).fold(
                onSuccess = { call.respond(it) },
                onError = { error ->
                    val e = error.failures().first()
                    when (e) {
                        is AuthService.Error.OAuthRedirectRequired -> {
                            val redirectUrl =
                                if (envConfig.mode == Config.Mode.PROD || envConfig.mode == Config.Mode.STAGING) {
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

        post("/login/google/android", AuthOpenAPISpec.loginGoogleAndroid) {
            val googleLoginRequest = call.receive<GoogleLoginRequest>()

            call.respondKIO(AuthService.loginGoogleUser(googleLoginRequest.idToken))
        }

        post("/login/apple", AuthOpenAPISpec.loginApple) {
            val appleLoginRequest = call.receive<AppleLoginRequest>()

            call.respondKIO(
                AuthService.loginAppleUser(appleLoginRequest)
            )
        }

        post("/apple/notification", { hidden = true }) {
            val notification = call.receive<AppleNotificationPayload>()

            call.respondKIO(
                AppleNotificationService.handleNotification(notification.payload).transact()
            )
        }

        post("/auth/refresh", AuthOpenAPISpec.refreshToken) {
            val refreshToken = call.receive<RefreshTokenRequest>()

            call.respondKIO(AuthService.refreshToken(refreshToken))
        }

        post("/magic-link", AuthOpenAPISpec.requestMagicLink) {
            val magicLinkRequest = call.receive<MagicLinkRequest>()

            call.respondKIO(AuthService.sendVerificationEmail(magicLinkRequest))
        }

        post("/magic-link/login", AuthOpenAPISpec.loginViaMagicLink) {
            val magicLinkLoginRequest = call.receive<MagicLinkLoginRequest>()

            call.respondKIO(AuthService.loginViaMagicLink(magicLinkLoginRequest))
        }

        post("/verify-email", AuthOpenAPISpec.verifyEmail) {
            val verifyEmailRequest = call.receive<VerifyEmailRequest>()

            call.respondKIO(AuthService.verifyEmail(verifyEmailRequest))
        }

        authenticate(Realm.COMMON) {
            post("/send-verification-email", AuthOpenAPISpec.sendVerificationEmail) {
                val user = call.principal<LoggedInUser>()!!

                call.respondKIO(EmailService.sendVerificationEmail(user.id).transact())
            }
        }
    }
}