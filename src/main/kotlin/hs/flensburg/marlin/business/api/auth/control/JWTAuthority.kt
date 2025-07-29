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
    private lateinit var ALGORITHM_ACCESS: Algorithm
    private lateinit var ALGORITHM_REFRESH: Algorithm
    private const val ACCESS_TTL_MILLIS: Long = 36_000_00L * 3 // 3 hours
    private const val REFRESH_TTL_IN_MS: Long = 36_000_00L * 24 * 30 // 30 days

    fun init(envConfig: Config) {
        ISSUER = envConfig.auth.jwtIssuer
        AUDIENCE = envConfig.auth.jwtAudience
        ALGORITHM_ACCESS = Algorithm.HMAC256(envConfig.auth.jwtSecretAccess)
        ALGORITHM_REFRESH = Algorithm.HMAC256(envConfig.auth.jwtSecretRefresh)
    }

    val accessVerifier: JWTVerifier by lazy {
        JWT.require(ALGORITHM_ACCESS)
            .withAudience(AUDIENCE)
            .withIssuer(ISSUER)
            .build()
    }

    val refreshVerifier: JWTVerifier by lazy {
        JWT.require(ALGORITHM_REFRESH)
            .withAudience(AUDIENCE)
            .withIssuer(ISSUER)
            .build()
    }

    fun generateAccessToken(user: User): String =
        generateToken(user, "Authorization Token", ALGORITHM_ACCESS, ACCESS_TTL_MILLIS)

    fun generateRefreshToken(user: User): String =
        generateToken(user, "Refresh Token", ALGORITHM_REFRESH, REFRESH_TTL_IN_MS)

    private fun generateToken(
        user: User,
        subject: String,
        algorithm: Algorithm,
        ttlMillis: Long
    ): String = JWT.create()
        .withSubject(subject)
        .withClaim("id", user.id)
        .withClaim("email", user.email)
        .withIssuer(ISSUER)
        .withAudience(AUDIENCE)
        .withExpiresAt(getExpiration(ttlMillis))
        .sign(algorithm)

    private fun getExpiration(ttlMillis: Long) = Date(System.currentTimeMillis() + ttlMillis)
}