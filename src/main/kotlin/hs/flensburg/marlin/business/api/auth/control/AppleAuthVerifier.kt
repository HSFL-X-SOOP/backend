package hs.flensburg.marlin.business.api.auth.control

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import de.lambda9.tailwind.core.KIO
import hs.flensburg.marlin.business.App
import hs.flensburg.marlin.business.api.auth.boundary.AuthService
import hs.flensburg.marlin.business.api.auth.entity.ApplePublicKeysResponse
import hs.flensburg.marlin.business.httpclient
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import java.math.BigInteger
import java.security.KeyFactory
import java.security.interfaces.RSAPublicKey
import java.security.spec.RSAPublicKeySpec
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger { }

object AppleAuthVerifier {
    private const val APPLE_PUBLIC_KEYS_URL = "https://appleid.apple.com/auth/keys"
    private const val ISSUER = "https://appleid.apple.com"
    private val publicKeysCache = ConcurrentHashMap<String, RSAPublicKey>()
    private var lastFetchTime: Long = 0
    private const val CACHE_DURATION_MS = 36_000_00L * 24 // 24 hours

    fun verifyAndDecodeToken(
        identityToken: String,
        expectedAudience: String
    ): App<AuthService.Error, DecodedJWT> = KIO.comprehension {
        val unverifiedJWT = try {
            JWT.decode(identityToken)
        } catch (e: Exception) {
            logger.warn(e) { "Failed to decode Apple identity token" }
            !KIO.fail(AuthService.Error.BadRequest)
        }

        val kid = unverifiedJWT.keyId
        if (kid.isNullOrBlank()) {
            logger.warn { "Apple identity token missing 'kid' (key ID) in header" }
            !KIO.fail(AuthService.Error.BadRequest)
        }

        val publicKey = !fetchPublicKey(kid)

        val algorithm = Algorithm.RSA256(publicKey, null)
        val verifier = JWT.require(algorithm)
            .withAudience(expectedAudience)
            .withIssuer(ISSUER)
            .build()

        val verifiedJWT = try {
            verifier.verify(identityToken)
        } catch (e: Exception) {
            logger.warn(e) { "Apple identity token verification failed: ${e.message}" }
            !KIO.fail(AuthService.Error.Unauthorized)
        }

        KIO.ok(verifiedJWT)
    }

    private fun fetchPublicKey(
        kid: String
    ): App<AuthService.Error, RSAPublicKey> = KIO.comprehension {
        val currentTime = System.currentTimeMillis()
        val shouldRefreshCache = (currentTime - lastFetchTime) > CACHE_DURATION_MS || publicKeysCache.isEmpty()

        if (shouldRefreshCache) {
            !refreshPublicKeys()
        }

        val publicKey = publicKeysCache[kid]
        if (publicKey == null) {
            logger.warn { "Apple public key with kid '$kid' not found in cache. Available keys: ${publicKeysCache.keys}" }
            !KIO.fail(AuthService.Error.Unauthorized)
        }

        KIO.ok(publicKey)
    }

    private fun refreshPublicKeys(): App<AuthService.Error, Unit> = KIO.comprehension {
        try {
            logger.info { "Fetching Apple public keys from $APPLE_PUBLIC_KEYS_URL" }

            val response: String = runBlocking { httpclient.get(APPLE_PUBLIC_KEYS_URL).body() }
            val keysResponse = Json.decodeFromString<ApplePublicKeysResponse>(response)

            publicKeysCache.clear()

            keysResponse.keys.forEach { key ->
                try {
                    val publicKey = buildRSAPublicKey(key.n, key.e)
                    publicKeysCache[key.kid] = publicKey
                    logger.debug { "Cached Apple public key: kid=${key.kid}, alg=${key.alg}" }
                } catch (e: Exception) {
                    logger.warn(e) { "Failed to build RSA public key for kid=${key.kid}" }
                }
            }

            lastFetchTime = System.currentTimeMillis()

            logger.info { "Successfully cached ${publicKeysCache.size} Apple public keys" }

            KIO.unit
        } catch (e: Exception) {
            logger.error(e) { "Failed to fetch Apple public keys: ${e.message}" }
            !KIO.fail(AuthService.Error.Unknown())
        }
    }

    private fun buildRSAPublicKey(nBase64: String, eBase64: String): RSAPublicKey {
        val decoder = Base64.getUrlDecoder()

        val nBytes = decoder.decode(nBase64)
        val eBytes = decoder.decode(eBase64)

        val modulus = BigInteger(1, nBytes)
        val exponent = BigInteger(1, eBytes)

        val spec = RSAPublicKeySpec(modulus, exponent)
        val keyFactory = KeyFactory.getInstance("RSA")

        return keyFactory.generatePublic(spec) as RSAPublicKey
    }
}
