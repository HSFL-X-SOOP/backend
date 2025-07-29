package hs.flensburg.marlin.plugins

import de.lambda9.tailwind.core.KIO
import hs.flensburg.marlin.business.JEnv
import hs.flensburg.marlin.business.api.auth.boundary.configureAuth
import io.ktor.server.application.Application
import io.ktor.server.routing.get
import io.ktor.server.routing.routing


fun Application.configureRouting(env: JEnv) {
    configureAuth(env.env.config)

    routing {
        get("/health") {
            call.respondKIO(KIO.ok("Marlin-Backend is running!"))
        }
    }
}