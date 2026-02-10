package hs.flensburg.marlin.business.api.apikey.control

import java.security.MessageDigest
import java.security.SecureRandom

object ApiKeyGenerator {

    private const val PREFIX = "mlk_"
    private const val KEY_BYTES = 32
    private val secureRandom = SecureRandom()

    fun generate(): Pair<String, String> {
        val bytes = ByteArray(KEY_BYTES)
        secureRandom.nextBytes(bytes)
        val hex = bytes.joinToString("") { "%02x".format(it) }
        val rawKey = "$PREFIX$hex"
        val keyPrefix = rawKey.substring(0, 12)
        return Pair(rawKey, keyPrefix)
    }

    fun hash(key: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(key.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    fun verify(key: String, storedHash: String): Boolean {
        return hash(key) == storedHash
    }
}
