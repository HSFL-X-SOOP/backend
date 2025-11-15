package hs.flensburg.marlin.plugins

import de.lambda9.tailwind.core.KIO
import de.lambda9.tailwind.core.KIO.Companion.unsafeRunSync
import de.lambda9.tailwind.core.extensions.exit.fold
import hs.flensburg.marlin.business.ApiError
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.util.AttributeKey
import hs.flensburg.marlin.business.JEnv
import hs.flensburg.marlin.business.ServiceLayerError
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.response.respondNullable

suspend inline fun <E, reified A> ApplicationCall.respondKIO(kio: KIO<JEnv, E, A>) {
    val exit = kio.unsafeRunSync(kioEnv)
    exit.fold(
        onError = {
            when (it) {
                is ApiError -> respond(it.statusCode, it.message)
                is ServiceLayerError -> {
                    it.toApiError().let { gnvError -> respond(gnvError.statusCode, gnvError.message) }
                }

                else -> respond(HttpStatusCode.InternalServerError)
            }
        },
        onDefect = {
            respond(HttpStatusCode.InternalServerError, it.message?: "")
        },
        onSuccess = {
            respondNullable(it)
        }
    )

}

private val kioEnv = AttributeKey<JEnv>("kioEnv")

val ApplicationCall.kioEnv
    get(): JEnv =
        application.attributes[hs.flensburg.marlin.plugins.kioEnv]

fun Application.configureKIO(env: JEnv) {
    attributes.put(kioEnv, env)
}

