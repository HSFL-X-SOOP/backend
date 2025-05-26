package hs.flensburg.soop.plugins

import hs.flensburg.soop.business.ApiError
import hs.flensburg.soop.business.Result
import hs.flensburg.soop.business.ServiceLayerError
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import io.ktor.server.response.respondNullable


/**
 * Responds to the client with a [Result] object.
 *
 * @param E The type of the result.
 * @param T The type of the error.
 */
suspend inline fun <E, reified T> ApplicationCall.respondResult(result: Result<E, T>) {
    when (result) {
        is Result.Success -> respondNullable(result.result)
        is Result.Failure -> {
            when (result.error) {
                is ApiError -> respond(result.error.statusCode, result.error.message)
                is ServiceLayerError -> {
                    result.error.toApiError().let { error -> respond(error.statusCode, error.message) }
                }

                else -> respond(HttpStatusCode.InternalServerError, result.error.toString())
            }
        }
    }
}