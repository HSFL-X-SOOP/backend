package hs.flensburg.marlin.business.api.auth.boundary

import de.lambda9.tailwind.core.KIO.Companion.unsafeRunSync
import hs.flensburg.marlin.Config
import hs.flensburg.marlin.business.api.auth.control.JWTAuthority
import hs.flensburg.marlin.business.api.auth.entity.LoggedInUser
import hs.flensburg.marlin.business.api.auth.entity.LoginRequest
import hs.flensburg.marlin.business.api.auth.entity.RefreshRequest
import hs.flensburg.marlin.business.api.auth.entity.RegisterRequest
import hs.flensburg.marlin.plugins.kioEnv
import hs.flensburg.marlin.plugins.respondKIO
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing

fun Application.configureAuth(envConfig: Config) {
    JWTAuthority.init(envConfig)

    install(Authentication) {
        jwt("common") {
            realm = "Common-Realm"
            verifier(JWTAuthority.accessVerifier)
            validate { credential ->
                AuthService.validateCommonRealmAccess(credential).unsafeRunSync(kioEnv).fold(
                    onSuccess = { it },
                    onError = { null }
                )
            }
        }
    }

    routing {
        post("/register") {
            val registerRequest = call.receive<RegisterRequest>()

            call.respondKIO(AuthService.register(registerRequest))
        }

        post("/login") {
            val loginRequest = call.receive<LoginRequest>()

            call.respondKIO(AuthService.login(loginRequest))
        }

        post("/auth/refresh") {
            val refreshToken = call.receive<RefreshRequest>()

            call.respondKIO(AuthService.refresh(refreshToken))
        }

        authenticate("common") {
            get("/whoami") {
                val principal = call.principal<LoggedInUser>()!!

                call.respondText { principal.email }
            }
        }
    }
}