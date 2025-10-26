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
import hs.flensburg.marlin.business.api.users.entity.UserProfileResponse
import hs.flensburg.marlin.database.generated.enums.UserAuthorityRole
import hs.flensburg.marlin.database.generated.tables.pojos.User
import hs.flensburg.marlin.database.generated.tables.records.UserRecord
import io.ktor.server.auth.OAuthAccessTokenResponse
import io.ktor.server.auth.jwt.JWTCredential
import java.time.LocalDateTime

object AuthService {
    sealed class Error(private val message: String) : ServiceLayerError {
        object Unauthorized : Error("Unauthorized access")
        object PasswordIncorrect : Error("Email or password incorrect")
        object BadRequest : Error("Bad request")
        object OAuthRedirectRequired : Error("Redirect required")
        object LoginLimitExceeded : Error("Login limit exceeded, please try again later")
        object Blacklisted : Error("You are temporarily blocked, please try again later")

        override fun toApiError(): ApiError {
            return when (this) {
                is Unauthorized -> ApiError.Unauthorized(message)
                is PasswordIncorrect -> ApiError.BadRequest(message)
                is BadRequest -> ApiError.BadRequest(message)
                is OAuthRedirectRequired -> ApiError.Unauthorized(message)
                is LoginLimitExceeded -> ApiError.TooManyRequests(message)
                is Blacklisted -> ApiError.TooManyRequests(message)
            }
        }
    }

    private const val MAX_FAILED_LOGIN_ATTEMPTS = 3

    fun register(credentials: RegisterRequest): App<Error, LoginResponse> = KIO.comprehension {
        val email = credentials.email
        val password = credentials.password
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
        val profile = !UserRepo.fetchProfileByUserId(user.id!!).orDie()

        KIO.ok(LoginResponse(accessToken, refreshToken, profile?.let { UserProfileResponse.from(it) }))
    }

    fun login(credentials: LoginRequest, ipAddress: String): App<ServiceLayerError, LoginResponse> = KIO.comprehension {
        val user = !UserRepo.fetchByEmail(credentials.email).orDie().onNullFail { Error.PasswordIncorrect }

        !KIO.failOn(user.password == null) { Error.OAuthRedirectRequired }

        !checkUserIsNotBlacklisted(user.id!!)

        val passwordValid = Hashing.verifyPassword(credentials.password, user.password!!)

        if (!passwordValid) {
            !AuthRepo.insertFailedLoginAttempt(user.id!!, ipAddress).orDie()

            !checkFailedLoginLimitExceeded(user.id!!, ipAddress)

            !KIO.fail(Error.PasswordIncorrect)
        }

        val accessToken = JWTAuthority.generateAccessToken(user)
        val refreshToken = if (credentials.rememberMe) JWTAuthority.generateRefreshToken(user) else null
        val profile = !UserRepo.fetchProfileByUserId(user.id!!).orDie()

        KIO.ok(LoginResponse(accessToken, refreshToken, profile?.let { UserProfileResponse.from(it) }))
    }

    fun loginGoogleUser(authResponse: OAuthAccessTokenResponse.OAuth2): App<Error, LoginResponse> = KIO.comprehension {
        val identificationToken = authResponse.extraParameters["id_token"]
        loginWithGoogleIdToken(identificationToken!!)
    }

    fun loginGoogleUser(idToken: String): App<Error, LoginResponse> = KIO.comprehension {
        loginWithGoogleIdToken(idToken)
    }

    fun loginViaMagicLink(magicLinkRequest: MagicLinkLoginRequest): App<Error, LoginResponse> = KIO.comprehension {
        val decodedJWT = try {
            JWTAuthority.magicLinkVerifier.verify(magicLinkRequest.token)
        } catch (e: Exception) {
            !KIO.fail(Error.BadRequest)
        }

        val userId = decodedJWT.getClaim("id").asLong()

        val user = !UserRepo.fetchById(userId).orDie().onNullFail { Error.BadRequest }

        val accessToken = JWTAuthority.generateAccessToken(user)
        val refreshToken = JWTAuthority.generateRefreshToken(user)
        val profile = !UserRepo.fetchProfileByUserId(user.id!!).orDie()

        KIO.ok(LoginResponse(accessToken, refreshToken, profile?.let { UserProfileResponse.from(it) }))
    }

    fun refreshToken(refreshTokenRequest: RefreshTokenRequest): App<Error, LoginResponse> = KIO.comprehension {
        val decodedJWT = try {
            JWTAuthority.refreshVerifier.verify(refreshTokenRequest.refreshToken)
        } catch (e: Exception) {
            !KIO.fail(Error.BadRequest)
        }

        val userId = decodedJWT.getClaim("id").asLong()
        val email = decodedJWT.getClaim("email").asString()

        !KIO.failOn(userId == null || email == null) { Error.BadRequest }

        val user = !UserRepo.fetchById(userId).orDie().onNullFail { Error.BadRequest }

        val newAccessToken = JWTAuthority.generateAccessToken(user)
        val newRefreshToken = JWTAuthority.generateRefreshToken(user)
        val profile = !UserRepo.fetchProfileByUserId(user.id!!).orDie()

        KIO.ok(LoginResponse(newAccessToken, newRefreshToken, profile?.let { UserProfileResponse.from(it) }))
    }

    fun verifyEmail(verifyEmailRequest: VerifyEmailRequest): App<Error, Unit> = KIO.comprehension {
        val decodedJWT = try {
            JWTAuthority.emailVerificationVerifier.verify(verifyEmailRequest.token)
        } catch (e: Exception) {
            !KIO.fail(Error.BadRequest)
        }

        val userId = decodedJWT.getClaim("id").asLong()

        !KIO.failOn(userId == null) { Error.BadRequest }

        val user = !UserRepo.fetchById(userId).orDie().onNullFail { Error.BadRequest }

        if (user.verified!!.not()) !UserRepo.setEmailIsVerified(user.id!!).orDie()

        KIO.unit
    }

    fun validateCommonRealmAccess(credentials: JWTCredential): App<Error, LoggedInUser> =
        validateRealmAccess(credentials)

    fun validateHarborControlRealmAccess(credentials: JWTCredential): App<Error, LoggedInUser> =
        validateRealmAccess(credentials) { user ->
            user.role in listOf(UserAuthorityRole.ADMIN, UserAuthorityRole.HARBOR_MASTER)
        }

    fun validateAdminRealmAccess(credentials: JWTCredential): App<Error, LoggedInUser> =
        validateRealmAccess(credentials) { user ->
            user.role == UserAuthorityRole.ADMIN
        }

    private fun validateRealmAccess(
        credentials: JWTCredential,
        hasAccess: ((User) -> Boolean)? = null
    ): App<Error, LoggedInUser> = KIO.comprehension {
        val userId = credentials.payload.getClaim("id").asLong()
        val email = credentials.payload.getClaim("email").asString()

        !KIO.failOn(userId == null || email == null) { Error.Unauthorized }

        val user = !UserRepo.fetchById(userId).orDie().onNullFail { Error.Unauthorized }

        if (hasAccess != null) {
            // Invert 'hasAccess', for better readability
            !KIO.failOn(!hasAccess(user)) { Error.Unauthorized }
        }

        KIO.ok(LoggedInUser(userId, email))
    }

    private fun loginWithGoogleIdToken(idToken: String): App<Error, LoginResponse> = KIO.comprehension {
        val credentials = try {
            JWT.decode(idToken)
        } catch (e: Exception) {
            !KIO.fail(Error.BadRequest)
        }

        val email = credentials.getClaim("email").asString()
        val emailVerified = credentials.getClaim("email_verified").asBoolean()

        !KIO.failOn(email.isNullOrBlank()) { Error.BadRequest }

        val user = (!UserRepo.fetchByEmail(email).orDie()).let {
            if (it == null) {
                val userRecord = UserRecord().apply {
                    this.email = email
                    this.verified = emailVerified ?: false
                }

                !UserRepo.insert(userRecord).orDie()
            } else {
                it
            }
        }

        val accessToken = JWTAuthority.generateAccessToken(user)
        val refreshToken = JWTAuthority.generateRefreshToken(user)
        val profile = !UserRepo.fetchProfileByUserId(user.id!!).orDie()

        KIO.ok(LoginResponse(accessToken, refreshToken, profile?.let { UserProfileResponse.from(it) }))
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
            !KIO.fail(Error.Blacklisted)
        }

        KIO.unit
    }
}