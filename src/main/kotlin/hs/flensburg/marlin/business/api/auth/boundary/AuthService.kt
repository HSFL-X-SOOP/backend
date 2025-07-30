package hs.flensburg.marlin.business.api.auth.boundary

import de.lambda9.tailwind.core.KIO
import de.lambda9.tailwind.core.extensions.kio.onNullFail
import de.lambda9.tailwind.core.extensions.kio.orDie
import hs.flensburg.marlin.business.ApiError
import hs.flensburg.marlin.business.App
import hs.flensburg.marlin.business.ServiceLayerError
import hs.flensburg.marlin.business.api.auth.control.Hashing
import hs.flensburg.marlin.business.api.auth.control.JWTAuthority
import hs.flensburg.marlin.business.api.auth.entity.LoggedInUser
import hs.flensburg.marlin.business.api.auth.entity.LoginRequest
import hs.flensburg.marlin.business.api.auth.entity.LoginResponse
import hs.flensburg.marlin.business.api.auth.entity.RefreshRequest
import hs.flensburg.marlin.business.api.auth.entity.RegisterRequest
import hs.flensburg.marlin.business.api.users.control.UserRepo
import hs.flensburg.marlin.database.generated.tables.records.UserRecord
import io.ktor.server.auth.jwt.JWTCredential

object AuthService {
    sealed class Error(private val message: String) : ServiceLayerError {
        object Unauthorized : Error("Unauthorized access")
        object BadRequest : Error("Bad request")

        override fun toApiError(): ApiError {
            return when (this) {
                is Unauthorized -> ApiError.Unauthorized(message)
                is BadRequest -> ApiError.BadRequest(message)
            }
        }
    }

    fun register(credentials: RegisterRequest): App<Error, LoginResponse> = KIO.comprehension {
        val email = credentials.email
        val password = credentials.password

        !KIO.failOn(email.isBlank() || password.isBlank()) { Error.BadRequest }

        val existingUser = !UserRepo.fetchByEmail(email).orDie()

        !KIO.failOn(existingUser != null) { Error.BadRequest }

        val hashedPassword = Hashing.hashPassword(password)

        val userRecord = UserRecord().apply {
            this.email = email
            this.password = hashedPassword
        }

        val user = !UserRepo.insert(userRecord).orDie()

        val accessToken = JWTAuthority.generateAccessToken(user)

        val refreshToken = if (credentials.rememberMe) JWTAuthority.generateRefreshToken(user) else null

        KIO.ok(LoginResponse(accessToken, refreshToken))
    }

    fun login(credentials: LoginRequest): App<Error, LoginResponse> = KIO.comprehension {
        val user = !UserRepo.fetchByEmail(credentials.email).orDie().onNullFail { Error.Unauthorized }

        Hashing.verifyPassword(credentials.password, user.password!!)

        val accessToken = JWTAuthority.generateAccessToken(user)

        val refreshToken = if (credentials.rememberMe) JWTAuthority.generateRefreshToken(user) else null

        KIO.ok(LoginResponse(accessToken, refreshToken))
    }

    fun refresh(refreshRequest: RefreshRequest): App<Error, LoginResponse> = KIO.comprehension {
        val decodedJWT = try {
            JWTAuthority.refreshVerifier.verify(refreshRequest.refreshToken)
        } catch (e: Exception) {
            !KIO.fail(Error.Unauthorized)
        }

        val userId = decodedJWT.getClaim("id").asLong()
        val email = decodedJWT.getClaim("email").asString()

        !KIO.failOn(userId == null || email == null) { Error.Unauthorized }

        val user = !UserRepo.fetchById(userId).orDie().onNullFail { Error.Unauthorized }

        val newAccessToken = JWTAuthority.generateAccessToken(user)
        val newRefreshToken = JWTAuthority.generateRefreshToken(user)

        KIO.ok(LoginResponse(newAccessToken, newRefreshToken))
    }

    fun validateCommonRealmAccess(credentials: JWTCredential): App<Error, LoggedInUser> = KIO.comprehension {
        val userId = credentials.payload.getClaim("id").asLong()
        val email = credentials.payload.getClaim("email").asString()

        !KIO.failOn(userId == null || email == null) { Error.Unauthorized }

        !UserRepo.fetchById(userId).orDie().onNullFail { Error.Unauthorized }

        KIO.ok(LoggedInUser(userId, email))
    }
}