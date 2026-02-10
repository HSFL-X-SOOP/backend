package hs.flensburg.marlin.business.api.apikey.boundary

import de.lambda9.tailwind.core.KIO
import de.lambda9.tailwind.core.extensions.kio.onNullFail
import de.lambda9.tailwind.core.extensions.kio.orDie
import hs.flensburg.marlin.business.ApiError
import hs.flensburg.marlin.business.App
import hs.flensburg.marlin.business.ServiceLayerError
import hs.flensburg.marlin.business.api.apikey.control.ApiKeyGenerator
import hs.flensburg.marlin.business.api.apikey.control.ApiKeyRepository
import hs.flensburg.marlin.business.api.apikey.entity.ApiKeyInfo
import hs.flensburg.marlin.business.api.apikey.entity.CreateApiKeyRequest
import hs.flensburg.marlin.business.api.apikey.entity.CreateApiKeyResponse
import hs.flensburg.marlin.business.api.apikey.entity.RevokeApiKeyResponse
import hs.flensburg.marlin.business.api.auth.entity.LoggedInUser
import hs.flensburg.marlin.business.api.subscription.control.SubscriptionRepository
import hs.flensburg.marlin.business.api.users.control.UserRepo
import hs.flensburg.marlin.database.generated.enums.SubscriptionType
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.UUID

object ApiKeyService {

    sealed class Error(private val message: String) : ServiceLayerError {
        object SubscriptionRequired : Error("Active API_ACCESS subscription required")
        object NotFound : Error("API key not found")
        object InvalidKey : Error("Invalid API key")
        object UserNotFound : Error("User not found")

        override fun toApiError(): ApiError {
            return when (this) {
                is SubscriptionRequired -> ApiError.Forbidden(message)
                is NotFound -> ApiError.NotFound(message)
                is InvalidKey -> ApiError.Unauthorized(message)
                is UserNotFound -> ApiError.NotFound(message)
            }
        }
    }

    private val logger = KotlinLogging.logger {}

    fun createApiKey(
        userId: Long,
        request: CreateApiKeyRequest
    ): App<ServiceLayerError, CreateApiKeyResponse> = KIO.comprehension {
        val hasSub = !SubscriptionRepository.hasActiveSubscription(userId, SubscriptionType.API_ACCESS).orDie()
        !KIO.failOn(!hasSub) { Error.SubscriptionRequired }

        !ApiKeyRepository.revokeAllForUser(userId).orDie()

        val (rawKey, prefix) = ApiKeyGenerator.generate()
        val keyHash = ApiKeyGenerator.hash(rawKey)

        val apiKey = !ApiKeyRepository.insert(userId, prefix, keyHash, request.name).orDie()

        logger.info { "Created API key with prefix $prefix for user $userId" }

        KIO.ok(
            CreateApiKeyResponse(
                id = apiKey.id.toString(),
                key = rawKey,
                prefix = prefix,
                name = apiKey.name,
                createdAt = apiKey.createdAt.toString()
            )
        )
    }

    fun listApiKeys(userId: Long): App<ServiceLayerError, List<ApiKeyInfo>> = KIO.comprehension {
        val keys = !ApiKeyRepository.findAllByUserId(userId).orDie()

        KIO.ok(
            keys.map { key ->
                ApiKeyInfo(
                    id = key.id.toString(),
                    prefix = key.keyPrefix!!,
                    name = key.name,
                    isActive = key.isActive!!,
                    lastUsedAt = key.lastUsedAt?.toString(),
                    createdAt = key.createdAt.toString()
                )
            }
        )
    }

    fun revokeApiKey(userId: Long, keyId: UUID): App<ServiceLayerError, RevokeApiKeyResponse> = KIO.comprehension {
        val keys = !ApiKeyRepository.findAllByUserId(userId).orDie()
        val key = keys.find { it.id == keyId }
        !KIO.failOn(key == null) { Error.NotFound }

        val revoked = !ApiKeyRepository.revokeById(keyId).orDie()

        KIO.ok(RevokeApiKeyResponse(revoked = revoked))
    }

    fun validateApiKey(rawKey: String): App<ServiceLayerError, LoggedInUser> = KIO.comprehension {
        val prefix = if (rawKey.length >= 12) rawKey.substring(0, 12) else {
            !KIO.fail(Error.InvalidKey)
        }

        val candidates = !ApiKeyRepository.findActiveByPrefix(prefix).orDie()

        val matchingKey = candidates.find { ApiKeyGenerator.verify(rawKey, it.keyHash!!) }

        !KIO.failOn(matchingKey == null) { Error.InvalidKey }

        val hasSub =
            !SubscriptionRepository.hasActiveSubscription(matchingKey!!.userId!!, SubscriptionType.API_ACCESS).orDie()

        !KIO.failOn(!hasSub) { Error.SubscriptionRequired }

        !ApiKeyRepository.updateLastUsed(matchingKey.id!!).orDie()

        val user = !UserRepo.fetchById(matchingKey.userId!!).orDie()
            .onNullFail { Error.UserNotFound }

        KIO.ok(LoggedInUser(user.id!!, user.email!!))
    }
}
