package hs.flensburg.marlin.business.api.auth.boundary

import com.auth0.jwt.JWT
import de.lambda9.tailwind.core.KIO
import de.lambda9.tailwind.core.extensions.kio.onNullFail
import de.lambda9.tailwind.core.extensions.kio.orDie
import hs.flensburg.marlin.business.ApiError
import hs.flensburg.marlin.business.App
import hs.flensburg.marlin.business.ServiceLayerError
import hs.flensburg.marlin.business.api.auth.control.AuthRepo
import hs.flensburg.marlin.business.api.auth.control.BlacklistHandler
import hs.flensburg.marlin.business.api.auth.control.Hashing
import hs.flensburg.marlin.business.api.auth.control.JWTAuthority
import hs.flensburg.marlin.business.api.auth.entity.LoggedInUser
import hs.flensburg.marlin.business.api.auth.entity.LoginRequest
import hs.flensburg.marlin.business.api.auth.entity.LoginResponse
import hs.flensburg.marlin.business.api.auth.entity.MagicLinkLoginRequest
import hs.flensburg.marlin.business.api.auth.entity.RefreshTokenRequest
import hs.flensburg.marlin.business.api.auth.entity.RegisterRequest
import hs.flensburg.marlin.business.api.auth.entity.VerifyEmailRequest
import hs.flensburg.marlin.business.api.users.control.UserRepo
import hs.flensburg.marlin.database.generated.tables.records.UserRecord
import io.ktor.server.auth.OAuthAccessTokenResponse
import io.ktor.server.auth.jwt.JWTCredential
import java.time.LocalDateTime

object AuthService {
    sealed class Error(private val message: String) : ServiceLayerError {
        object Unauthorized : Error("Unauthorized access")
        object BadRequest : Error("Bad request")
        object OAuthRedirectRequired : Error("Redirect required")
        object LoginLimitExceeded : Error("Login limit exceeded, please try again later")

        override fun toApiError(): ApiError {
            return when (this) {
                is Unauthorized -> ApiError.Unauthorized(message)
                is BadRequest -> ApiError.BadRequest(message)
                is OAuthRedirectRequired -> ApiError.Unauthorized(message)
                is LoginLimitExceeded -> ApiError.TooManyRequests(message)
            }
        }
    }

    private const val MAX_FAILED_LOGIN_ATTEMPTS = 3

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
            this.verified = false
        }

        val user = !UserRepo.insert(userRecord).orDie()

        val accessToken = JWTAuthority.generateAccessToken(user)
        val refreshToken = if (credentials.rememberMe) JWTAuthority.generateRefreshToken(user) else null

        KIO.ok(LoginResponse(accessToken, refreshToken))
    }

    fun login(credentials: LoginRequest, ipAddress: String): App<ServiceLayerError, LoginResponse> = KIO.comprehension {
        val user = !UserRepo.fetchByEmail(credentials.email).orDie().onNullFail { Error.Unauthorized }

        !KIO.failOn(user.password == null) { Error.OAuthRedirectRequired }

        !checkUserIsNotBlacklisted(user.id!!)

        val passwordValid = Hashing.verifyPassword(credentials.password, user.password!!)

        if (!passwordValid) {
            !AuthRepo.insertFailedLoginAttempt(user.id!!, ipAddress).orDie()

            !checkFailedLoginLimitExceeded(user.id!!, ipAddress)

            !KIO.fail(Error.Unauthorized)
        }

        val accessToken = JWTAuthority.generateAccessToken(user)
        val refreshToken = if (credentials.rememberMe) JWTAuthority.generateRefreshToken(user) else null

        KIO.ok(LoginResponse(accessToken, refreshToken))
    }

    fun loginGoogleUser(authResponse: OAuthAccessTokenResponse.OAuth2): App<Error, LoginResponse> = KIO.comprehension {
        val identificationToken = authResponse.extraParameters["id_token"]

        val credentials = JWT.decode(identificationToken)
        val email = credentials.getClaim("email").asString()

        val user = (!UserRepo.fetchByEmail(email).orDie()).let {
            if (it == null) {
                val userRecord = UserRecord().apply {
                    this.email = email
                    this.verified = credentials.getClaim("email_verified").asBoolean()
                }

                !UserRepo.insert(userRecord).orDie()
            } else {
                it
            }
        }

        val accessToken = JWTAuthority.generateAccessToken(user)
        val refreshToken = JWTAuthority.generateRefreshToken(user)

        KIO.ok(LoginResponse(accessToken, refreshToken))
    }

    fun loginViaMagicLink(magicLinkRequest: MagicLinkLoginRequest): App<Error, LoginResponse> = KIO.comprehension {
        val decodedJWT = try {
            JWTAuthority.magicLinkVerifier.verify(magicLinkRequest.token)
        } catch (e: Exception) {
            !KIO.fail(Error.Unauthorized)
        }

        val userId = decodedJWT.getClaim("id").asLong()

        val user = !UserRepo.fetchById(userId).orDie().onNullFail { Error.Unauthorized }

        val accessToken = JWTAuthority.generateAccessToken(user)
        val refreshToken = JWTAuthority.generateRefreshToken(user)

        KIO.ok(LoginResponse(accessToken, refreshToken))
    }

    fun refreshToken(refreshTokenRequest: RefreshTokenRequest): App<Error, LoginResponse> = KIO.comprehension {
        val decodedJWT = try {
            JWTAuthority.refreshVerifier.verify(refreshTokenRequest.refreshToken)
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

    fun verifyEmail(verifyEmailRequest: VerifyEmailRequest): App<Error, Unit> = KIO.comprehension {
        val decodedJWT = try {
            JWTAuthority.emailVerificationVerifier.verify(verifyEmailRequest.token)
        } catch (e: Exception) {
            !KIO.fail(Error.Unauthorized)
        }

        val userId = decodedJWT.getClaim("id").asLong()

        !KIO.failOn(userId == null) { Error.Unauthorized }

        val user = !UserRepo.fetchById(userId).orDie().onNullFail { Error.Unauthorized }

        if (user.verified!!.not()) !UserRepo.setEmailIsVerified(user.id!!).orDie()

        KIO.unit
    }

    fun validateCommonRealmAccess(credentials: JWTCredential): App<Error, LoggedInUser> = KIO.comprehension {
        val userId = credentials.payload.getClaim("id").asLong()
        val email = credentials.payload.getClaim("email").asString()

        !KIO.failOn(userId == null || email == null) { Error.Unauthorized }

        !UserRepo.fetchById(userId).orDie().onNullFail { Error.Unauthorized }

        KIO.ok(LoggedInUser(userId, email))
    }

    private fun checkFailedLoginLimitExceeded(
        userId: Long,
        ipAddress: String
    ): App<ServiceLayerError, Unit> = KIO.comprehension {
        val failedLoginAttempts = !AuthRepo.fetchFailedLoginAttempts(userId).orDie()

        if (failedLoginAttempts.size >= MAX_FAILED_LOGIN_ATTEMPTS) {
            !BlacklistHandler.addUserToBlacklist(userId, ipAddress)
            !KIO.fail(Error.LoginLimitExceeded)
        } else {
            KIO.unit
        }
    }

    private fun checkUserIsNotBlacklisted(
        userId: Long
    ): App<Error, Unit> = KIO.comprehension {
        val blacklist = !AuthRepo.fetchUserLoginBlacklist(userId).orDie()

        if (blacklist != null && blacklist.blockedUntil!!.isAfter(LocalDateTime.now())) {
            !KIO.fail(Error.LoginLimitExceeded)
        }

        KIO.unit
    }
}