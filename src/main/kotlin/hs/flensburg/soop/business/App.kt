package main.kotlin.hs.flensburg.soop.business

import main.kotlin.hs.flensburg.soop.Config
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import main.kotlin.hs.flensburg.soop.business.Env.dslContext
import io.ktor.http.HttpStatusCode
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.exception.DataAccessException
import org.jooq.impl.DSL


/**
 * The [Env] object contains all dependencies necessary to start the application.
 */
object Env {
    /** A global DSLContext for the application.*/
    lateinit var dslContext: DSLContext

    /**
     * This function is called on application startup to configure the Database connection.
     *
     * @param config The configuration object containing the passed environment variables.
     * @return An instance of [AppEnvironment] containing the configuration, DSLContext, and DataSource.
     */
    fun configure(config: Config): AppEnvironment {
        val ds = HikariConfig().apply {
            jdbcUrl = config.database.url
            username = config.database.user
            password = config.database.password
            driverClassName = "org.postgresql.Driver"
        }.let(::HikariDataSource)

        dslContext = DSL.using(ds, SQLDialect.POSTGRES)

        return AppEnvironment(
            config = config,
            dslContext = dslContext,
            dataSource = ds
        )
    }

    data class AppEnvironment(val config: Config, val dslContext: DSLContext, val dataSource: HikariDataSource)
}


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


/**
 * The [Jooq] object provides utility functions to execute queries using jOOQ.
 */
object Jooq {
    /**
     * This function is used to execute a query to the database.
     *
     * @param query A lambda function that takes a DSLContext and returns a result of type T. Inside the lambda,
     * you can build and execute a jooq query.
     * @return A Result object containing either a DataAccessException or the result of type T.
     */
    fun <T> query(query: DSLContext.() -> T): Result<DataAccessException, T> {
        return try {
            Result.ok(query.invoke(dslContext))
        } catch (e: DataAccessException) {
            Result.failure(e)
        }
    }

    /**
     * This function is used to execute a transactional query to the database.
     *
     * @param query A lambda function that takes a DSLContext and returns a result of type T. Inside the lambda,
     * you can build and execute a jooq query.
     * @return A Result object containing either a DataAccessException or the result of type T.
     */
    fun <T> transactionalQuery(query: DSLContext.() -> T): Result<DataAccessException, T> {
        return try {
            dslContext.transactionResult { transactionConfiguration ->
                val txDslContext = DSL.using(transactionConfiguration)
                Result.ok(query.invoke(txDslContext))
            }
        } catch (e: DataAccessException) {
            Result.failure(e)
        }
    }
}


/**
 * A sealed class representing the result of an operation. It can either be a success or a failure.
 *
 * @param E The type of the error.
 * @param T The type of the success result.
 */
sealed class Result<out E, out T> {
    data class Success<E, T>(val result: T) : Result<E, T>()
    data class Failure<E, T>(val error: E) : Result<E, T>()

    fun isSuccess(): Boolean = this is Success
    fun isFailure(): Boolean = this is Failure

    fun getOrNull(): T? = (this as? Success<E, T>)?.result

    companion object {
        fun <E, T> ok(result: T): Result<E, T> = Success(result)
        fun <E, T> failure(error: E): Result<E, T> = Failure(error)
    }
}

/*
data class Comprehension<E, T>(
    val fn: () -> Result<E, T>,
) {
    fun run(): Result<E, T> {
        return fn.invoke()
    }

    fun <R> fold(
        onSuccess: (T) -> R,
        onFailure: (E) -> R,
    ): R {
        return when (val result = fn.invoke()) {
            is Result.Success -> onSuccess(result.result)
            is Result.Failure -> onFailure(result.error)
        }
    }
}*/
