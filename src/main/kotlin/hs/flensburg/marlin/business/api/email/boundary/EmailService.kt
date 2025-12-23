package hs.flensburg.marlin.business.api.email.boundary

import de.lambda9.tailwind.core.KIO
import de.lambda9.tailwind.core.extensions.kio.onNullFail
import de.lambda9.tailwind.core.extensions.kio.orDie
import de.lambda9.tailwind.jooq.transact
import hs.flensburg.marlin.business.ApiError
import hs.flensburg.marlin.business.App
import hs.flensburg.marlin.business.ServiceLayerError
import hs.flensburg.marlin.business.api.auth.control.AuthRepo
import hs.flensburg.marlin.business.api.email.control.EmailHandler
import hs.flensburg.marlin.business.api.email.control.EmailRepo
import hs.flensburg.marlin.business.api.auth.entity.Platform
import hs.flensburg.marlin.database.generated.enums.EmailType
import hs.flensburg.marlin.database.generated.tables.pojos.Email
import hs.flensburg.marlin.database.generated.tables.records.EmailRecord
import java.time.LocalDateTime

object EmailService {
    sealed class Error(private val message: String) : ServiceLayerError {
        data class AlreadySent(val userId: Long) : Error("An email has already been sent to this user recently")
        data class UserNotFound(val email: String) : Error("User with email $email not found")
        data class EmailSendingFailed(val emailId: Long, val error: String) :
            Error("Failed to send email with ID $emailId: $error")

        data class BlacklistEntryNotFound(val userId: Long) :
            Error("Blacklist entry for user with ID $userId not found")

        override fun toApiError(): ApiError {
            return when (this) {
                is AlreadySent -> ApiError.Conflict(message)
                is UserNotFound -> ApiError.NotFound(message)
                is EmailSendingFailed -> ApiError.Unknown(message)
                is BlacklistEntryNotFound -> ApiError.NotFound(message)
            }
        }
    }

    fun sendWelcomeEmail(userId: Long): App<Error, Unit> = KIO.comprehension {
        !checkNoConsecutiveEmails(userId, EmailType.WELCOME) { lastEmail ->
            lastEmail == null
        }

        val email = EmailRecord().apply {
            this.userId = userId
            this.type = EmailType.WELCOME
            this.sentAt = null
        }

        val res = !EmailRepo.insert(email).orDie()

        !EmailHandler.sendEmail(res).mapError { Error.EmailSendingFailed(res.id!!, it.toApiError().message) }

        KIO.unit
    }

    fun sendVerificationEmail(userId: Long): App<Error, Unit> = KIO.comprehension {
        !checkNoConsecutiveEmails(userId, EmailType.EMAIL_VERIFICATION) { lastEmail ->
            lastEmail == null || lastEmail.sentAt != null && lastEmail.sentAt!!.isBefore(
                LocalDateTime.now().minusDays(1)
            )
        }

        val email = EmailRecord().apply {
            this.userId = userId
            this.type = EmailType.EMAIL_VERIFICATION
            this.sentAt = null
        }

        val res = !EmailRepo.insert(email).orDie()

        !EmailHandler.sendEmail(res).mapError { Error.EmailSendingFailed(res.id!!, it.toApiError().message) }

        KIO.unit
    }

    fun sendMagicLinkEmail(userId: Long, platform: Platform): App<Error, Unit> = KIO.comprehension {
        !checkNoConsecutiveEmails(userId, EmailType.MAGIC_LINK) { lastEmail ->
            lastEmail == null || lastEmail.sentAt != null && lastEmail.sentAt!!.isBefore(
                LocalDateTime.now().minusMinutes(30)
            )
        }

        val email = EmailRecord().apply {
            this.userId = userId
            this.type = EmailType.MAGIC_LINK
            this.sentAt = null
        }

        val res = !EmailRepo.insert(email).orDie()

        !EmailHandler.sendEmail(res, platform).mapError { Error.EmailSendingFailed(res.id!!, it.toApiError().message) }

        KIO.unit
    }.transact()

    fun sendBlacklistNotificationEmail(userId: Long): App<Error, Unit> = KIO.comprehension {
        !checkNoConsecutiveEmails(userId, EmailType.TOO_MANY_FAILED_LOGIN_ATTEMPTS) { lastEmail ->
            lastEmail == null || lastEmail.sentAt != null && lastEmail.sentAt!!.isBefore(
                LocalDateTime.now().minusDays(15)
            )
        }

        val blacklistEntry = !AuthRepo.fetchUserLoginBlacklist(userId).orDie().onNullFail {
            Error.BlacklistEntryNotFound(userId)
        }

        val email = EmailRecord().apply {
            this.userId = userId
            this.type = EmailType.TOO_MANY_FAILED_LOGIN_ATTEMPTS
            this.sentAt = null
        }

        val res = !EmailRepo.insert(email).orDie()

        val infoFields = buildList {
            blacklistEntry.ipAddress?.let {
                add("The IP address from which the failed login attempts were made" to it)
            }
            blacklistEntry.country?.let {
                add("The country from which the failed login attempts were made" to it)
            }
            blacklistEntry.city?.let {
                add("The city from which the failed login attempts were made" to it)
            }
            blacklistEntry.region?.let {
                add("The region from which the failed login attempts were made" to it)
            }
        }

        !EmailHandler.sendEmail(
            email = res,
            infoFields = infoFields.toTypedArray()
        ).mapError { Error.EmailSendingFailed(res.id!!, it.toApiError().message) }

        KIO.unit
    }.transact()

    private fun checkNoConsecutiveEmails(
        userId: Long,
        type: EmailType,
        predicate: (Email?) -> Boolean
    ): App<Error, Unit> = KIO.comprehension {
        val lastEmail = !EmailRepo.fetchLastByUserAndType(userId, type).orDie()

        if (predicate(lastEmail)) {
            KIO.unit
        } else {
            !KIO.fail(Error.AlreadySent(userId))
        }
    }
}