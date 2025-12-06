package hs.flensburg.marlin.business

import hs.flensburg.marlin.Config
import de.lambda9.tailwind.core.KIO
import de.lambda9.tailwind.jooq.Jooq
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import kotlinx.serialization.Serializable
import org.jooq.Condition
import org.jooq.Field
import org.jooq.OrderField
import org.jooq.Record
import org.jooq.UpdateSetMoreStep
import org.jooq.UpdateSetStep
import org.jooq.impl.DSL
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

            return Jooq.create(
                env = env,
                config = Jooq.Config(
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
    data class Unauthorized(val msg: String?) : ApiError(HttpStatusCode.Unauthorized, msg ?: "Not authorized")
    data class NotFound(val msg: String?) : ApiError(HttpStatusCode.NotFound, msg ?: "Not found")
    data class BadRequest(val msg: String?) : ApiError(HttpStatusCode.BadRequest, msg ?: "Bad request")
    data class AlreadyExists(val msg: String?) : ApiError(HttpStatusCode.Conflict, msg ?: "Already exists")
    data class Unknown(val msg: String?) : ApiError(HttpStatusCode.InternalServerError, msg ?: "Unknown error")
    data class Conflict(val msg: String?) : ApiError(HttpStatusCode.Conflict, msg ?: "Conflict error")
    data class TooManyRequests(val msg: String?) : ApiError(HttpStatusCode.TooManyRequests, msg ?: "Too many requests")

    // Add more error types as needed

}

/**
 * The [ServiceLayerError] interface represents an error that can occur in the service layer.
 * It provides a method to convert the error to an [ApiError].
 */
interface ServiceLayerError {
    fun toApiError(): ApiError
}

/**
 * The [PageResult] class represents a paginated result set.
 *
 * @param A The type of items in the result set.
 * @param items The list of items in the current page.
 * @param filteredCount The total number of items after applying filters.
 * @param totalCount The total number of items without any filters.
 */
@Serializable
data class PageResult<A>(val items: List<A>, val filteredCount: Long, val totalCount: Long)

/**
 * The [Page] class represents pagination parameters for API requests.
 *
 * @param T The type of query parameters.
 * @param parameterWrapper The query parameters wrapped in a [Conditional] object.
 * @param order The sorting criteria.
 * @param limit The maximum number of items to return.
 * @param offset The number of items to skip.
 */
data class Page<T : Conditional>(val parameterWrapper: T, val order: OrderBy, val limit: Int, val offset: Int) {

    init {
        require(limit >= 0) { "Limit must be non-negative" }
        require(offset >= 0) { "Offset must be non-negative" }
        require(limit <= 1000) { "Limit exceeds maximum of 1000" }
    }

    companion object {
        inline fun <reified T : Conditional> from(queryParams: Parameters, factory: ConditionalFactory<T>): Page<T> {
            return Page(
                parameterWrapper = factory.from(queryParams),
                order = OrderBy.parse(queryParams["sort"] ?: "id.asc"),
                limit = queryParams["limit"]?.toIntOrNull() ?: 50,
                offset = queryParams["offset"]?.toIntOrNull() ?: 0
            )
        }

        inline fun <reified T : Conditional> from(queryParams: Parameters, parameterWrapper: T): Page<T> {
            return Page(
                parameterWrapper = parameterWrapper,
                order = OrderBy.parse(queryParams["sort"] ?: "id.asc"),
                limit = queryParams["limit"]?.toIntOrNull() ?: 50,
                offset = queryParams["offset"]?.toIntOrNull() ?: 0
            )
        }
    }
}

/**
 * The [OrderBy] class represents sorting criteria for API requests.
 *
 * @param orderBy The field to sort by.
 * @param ascending Whether the sorting should be in ascending order.
 */
data class OrderBy(val orderBy: String, val ascending: Boolean) {

    fun toOrderField(): OrderField<*> {
        return if (ascending) {
            DSL.field(DSL.name(orderBy)).asc()
        } else {
            DSL.field(DSL.name(orderBy)).desc()
        }
    }

    companion object {
        fun parse(sortParam: String): OrderBy {
            val parts = sortParam.split('.', limit = 2)
            val sortBy = parts[0].trim()
            val ascending = if (parts.size == 2) {
                when (parts[1].trim().lowercase()) {
                    "asc" -> true
                    "desc" -> false
                    else -> true
                }
            } else {
                true
            }
            return OrderBy(orderBy = sortBy, ascending = ascending)
        }
    }
}

/**
 * The [Conditional] interface represents a set of "conditions" that can be converted to a [Condition].
 */
interface Conditional {
    fun toCondition(): Condition
}

/**
 * The [ConditionalFactory] interface provides a method to create instances of [Conditional] from query parameters.
 *
 * @param T The type of [Conditional] to create.
 */
interface ConditionalFactory<T : Conditional> {
    fun from(queryParams: Parameters): T
}

/**
 * Sets the field to the given value if the value is not null.
 *
 * @param field The field to set.
 * @param value The value to set.
 * @return The updated [UpdateSetMoreStep] if the value is not null, otherwise
 */
fun <T, R : Record> UpdateSetStep<R>.setIfNotNull(
    field: Field<T>,
    value: T
): UpdateSetMoreStep<R> {
    return if (value != null) {
        this.set(field, value)
    } else {
        this as UpdateSetMoreStep<R>
    }
}

/**
 * Sets the field to the given value if the predicate returns true for the value.
 *
 * @param field The field to set.
 * @param value The value to set.
 * @param predicate The predicate to evaluate the value.
 * @return The updated [UpdateSetMoreStep] if the predicate returns true, otherwise
 */
fun <T, R : Record> UpdateSetStep<R>.setWhen(
    field: Field<T>,
    value: T,
    predicate: (T) -> Boolean
): UpdateSetMoreStep<R> {
    return if (predicate(value)) {
        this.set(field, value)
    } else {
        this as UpdateSetMoreStep<R>
    }
}