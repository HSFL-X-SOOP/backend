package hs.flensburg.marlin.business.api.auth.control

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import hs.flensburg.marlin.Config
import hs.flensburg.marlin.database.generated.tables.pojos.User
import java.util.Date

object JWTAuthority {
    private lateinit var ISSUER: String
    private lateinit var AUDIENCE: String
    private lateinit var ALGORITHM: Algorithm
    private const val ACCESS_TTL_MILLIS: Long = 36_000_00L * 3 // 3 hours
    private const val REFRESH_TTL_IN_MS: Long = 36_000_00L * 24 * 30 // 30 days
    private const val PASSWORD_RESET_TTL_IN_MS: Long = 36_000_00L / 2 // 30 minutes
    private const val MAGIC_LINK_TTL_IN_MS: Long = 36_000_00L / 2 // 30 minutes
    private const val EMAIL_VERIFICATION_TTL_IN_MS: Long = 36_000_00L * 24 // 24 hours

    private const val AUTH_SUBJECT = "Authorization Token"
    private const val REFRESH_SUBJECT = "Refresh Token"
    private const val MAGIC_LINK_SUBJECT = "Magic Link"
    private const val EMAIL_VERIFICATION_SUBJECT = "Email Verification"

    fun init(envConfig: Config) {
        ISSUER = envConfig.auth.jwtIssuer
        AUDIENCE = envConfig.auth.jwtAudience
        ALGORITHM = Algorithm.HMAC256(envConfig.auth.jwtSecretAccess)
    }

    val accessVerifier: JWTVerifier by lazy {
        JWT.require(ALGORITHM)
            .withAudience(AUDIENCE)
            .withIssuer(ISSUER)
            .withSubject(AUTH_SUBJECT)
            .build()
    }

    val refreshVerifier: JWTVerifier by lazy {
        JWT.require(ALGORITHM)
            .withAudience(AUDIENCE)
            .withIssuer(ISSUER)
            .withSubject(REFRESH_SUBJECT)
            .build()
    }

    val magicLinkVerifier: JWTVerifier by lazy {
        JWT.require(ALGORITHM)
            .withAudience(AUDIENCE)
            .withIssuer(ISSUER)
            .withSubject(MAGIC_LINK_SUBJECT)
            .build()
    }

    val emailVerificationVerifier: JWTVerifier by lazy {
        JWT.require(ALGORITHM)
            .withAudience(AUDIENCE)
            .withIssuer(ISSUER)
            .withSubject(EMAIL_VERIFICATION_SUBJECT)
            .build()
    }

    fun generateAccessToken(user: User): String =
        generateToken(user, AUTH_SUBJECT, ACCESS_TTL_MILLIS)

    fun generateRefreshToken(user: User): String =
        generateToken(user, REFRESH_SUBJECT, REFRESH_TTL_IN_MS)

    fun generateMagicLinkToken(user: User): String =
        generateToken(user, MAGIC_LINK_SUBJECT, MAGIC_LINK_TTL_IN_MS)

    fun generateEmailVerificationToken(user: User): String =
        generateToken(user, EMAIL_VERIFICATION_SUBJECT, EMAIL_VERIFICATION_TTL_IN_MS)

    private fun generateToken(
        user: User,
        subject: String,
        ttlMillis: Long
    ): String = JWT.create()
        .withSubject(subject)
        .withClaim("id", user.id)
        .withClaim("email", user.email)
        .withIssuer(ISSUER)
        .withAudience(AUDIENCE)
        .withExpiresAt(getExpiration(ttlMillis))
        .sign(ALGORITHM)

    private fun getExpiration(ttlMillis: Long) = Date(System.currentTimeMillis() + ttlMillis)
}