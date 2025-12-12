package hs.flensburg.marlin.business.api.auth.boundary

import de.lambda9.tailwind.core.KIO
import de.lambda9.tailwind.core.extensions.kio.orDie
import hs.flensburg.marlin.business.ApiError
import hs.flensburg.marlin.business.App
import hs.flensburg.marlin.business.JEnv
import hs.flensburg.marlin.business.ServiceLayerError
import hs.flensburg.marlin.business.api.auth.control.AppleAuthVerifier
import hs.flensburg.marlin.business.api.auth.entity.AppleNotificationEvent
import hs.flensburg.marlin.business.api.auth.entity.AppleNotificationType
import hs.flensburg.marlin.business.api.users.control.UserRepo
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.json.Json

private val logger = KotlinLogging.logger { }

object AppleNotificationService {
    sealed class Error(private val message: String) : ServiceLayerError {
        data object BadRequest : Error("Invalid notification payload")
        data object Unauthorized : Error("Failed to verify notification signature")

        override fun toApiError(): ApiError {
            return when (this) {
                is BadRequest -> ApiError.BadRequest(message)
                is Unauthorized -> ApiError.Unauthorized(message)
            }
        }
    }

    private val json = Json { ignoreUnknownKeys = true }

    fun handleNotification(payload: String): App<ServiceLayerError, Unit> = KIO.comprehension {
        val appleConfig = (!KIO.access<JEnv>()).env.config.appleAuth

        val verifiedJWT = !AppleAuthVerifier.verifyAndDecodeToken(
            identityToken = payload,
            expectedAudience = appleConfig.clientId
        ).mapError { Error.Unauthorized }

        val eventsClaim = verifiedJWT.getClaim("events").asString()
        if (eventsClaim.isNullOrBlank()) {
            logger.warn { "Apple notification missing 'events' claim" }
            !KIO.fail(Error.BadRequest)
        }

        val events = try {
            json.decodeFromString<AppleNotificationEvent>(eventsClaim)
        } catch (e: Exception) {
            logger.warn(e) { "Failed to parse Apple notification events: $eventsClaim" }
            !KIO.fail(Error.BadRequest)
        }

        logger.info { "Received Apple notification: type=${events.type}, sub=${events.sub}" }

        when (events.type) {
            AppleNotificationType.CONSENT_REVOKED -> !handleConsentRevoked(events.sub)
            AppleNotificationType.ACCOUNT_DELETE -> !handleAccountDelete(events.sub)
            AppleNotificationType.EMAIL_DISABLED -> !handleEmailDisabled(events.sub)
            AppleNotificationType.EMAIL_ENABLED -> !handleEmailEnabled(events.sub)
        }

        KIO.unit
    }

    private fun handleConsentRevoked(appleUserId: String): App<ServiceLayerError, Unit> = KIO.comprehension {
        logger.info { "User revoked consent for Apple Sign-In: $appleUserId" }

        val user = !UserRepo.fetchByAppleUserId(appleUserId).orDie()

        if (user != null) {
            !UserRepo.clearAppleUserId(appleUserId).orDie()
            logger.info { "Cleared Apple user ID for user ${user.id}" }
        } else {
            logger.warn { "No user found with Apple user ID: $appleUserId" }
        }

        KIO.unit
    }

    private fun handleAccountDelete(appleUserId: String): App<ServiceLayerError, Unit> = KIO.comprehension {
        logger.info { "User deleted their Apple account or stopped using Sign in with Apple: $appleUserId" }

        val user = !UserRepo.fetchByAppleUserId(appleUserId).orDie()

        if (user != null) {
            !UserRepo.clearAppleUserId(appleUserId).orDie()
            logger.info { "Cleared Apple user ID for user ${user.id}" }
        } else {
            logger.warn { "No user found with Apple user ID: $appleUserId" }
        }

        KIO.unit
    }

    private fun handleEmailDisabled(appleUserId: String): App<ServiceLayerError, Unit> = KIO.comprehension {
        logger.info { "User disabled email forwarding: $appleUserId" }

        val user = !UserRepo.fetchByAppleUserId(appleUserId).orDie()

        if (user != null) {
            !UserRepo.setEmailVerified(user.id!!, false).orDie()
            logger.info { "Set email verified to false for user ${user.id}" }
        } else {
            logger.warn { "No user found with Apple user ID: $appleUserId" }
        }

        KIO.unit
    }

    private fun handleEmailEnabled(appleUserId: String): App<ServiceLayerError, Unit> = KIO.comprehension {
        logger.info { "User re-enabled email forwarding: $appleUserId" }

        val user = !UserRepo.fetchByAppleUserId(appleUserId).orDie()

        if (user != null) {
            !UserRepo.setEmailVerified(user.id!!, true).orDie()
            logger.info { "Set email verified to true for user ${user.id}" }
        } else {
            logger.warn { "No user found with Apple user ID: $appleUserId" }
        }

        KIO.unit
    }
}
