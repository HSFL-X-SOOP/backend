package hs.flensburg.marlin.business

import hs.flensburg.marlin.Config
import de.lambda9.tailwind.core.KIO
import de.lambda9.tailwind.jooq.Jooq
import io.ktor.http.HttpStatusCode
import javax.sql.DataSource


/**
 * The [Env] object contains all dependencies necessary to start the application.
 */
data class Env(val config: Config) {
    companion object {
        /**
         * Creates a new [Env] for this application.
         * @param config a configuration
         */
        fun configure(config: Config): Pair<JEnv, DataSource> {
            val env = Env(
                config = config
            )

            return de.lambda9.tailwind.jooq.Jooq.create(
                env = env,
                config = de.lambda9.tailwind.jooq.Jooq.Config(
                    url = config.database.url,
                    user = config.database.user,
                    password = config.database.password,
                    schema = "marlin",
                )
            )
        }
    }
}

typealias JEnv = Jooq<Env>

typealias App<E, A> = KIO<JEnv, E, A>

/**
 * The [ApiError] class represents an error that can occur in the application.
 *
 * @param statusCode The HTTP status code associated with the error.
 * @param message A message describing the error.
 */
open class ApiError(
    val statusCode: HttpStatusCode,
    val message: String,
) {
    data class Unauthorized(val msg: String?) : ApiError(HttpStatusCode.Unauthorized, msg ?: "Nicht autorisert")
    data class NotFound(val msg: String?) : ApiError(HttpStatusCode.NotFound, msg ?: "Nicht gefunden")
    data class BadRequest(val msg: String?) : ApiError(HttpStatusCode.BadRequest, msg ?: "Fehlerhafte Anfrage")
    data class AlreadyExists(val msg: String?) : ApiError(HttpStatusCode.Conflict, msg ?: "Bereits vorhanden")
    data class Unknown(val msg: String?) : ApiError(HttpStatusCode.InternalServerError, msg ?: "Unbekannter Fehler")

    // Add more error types as needed

}

/**
 * The [ServiceLayerError] interface represents an error that can occur in the service layer.
 * It provides a method to convert the error to an [ApiError].
 */
interface ServiceLayerError {
    fun toApiError(): ApiError
}

