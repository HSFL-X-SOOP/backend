package hs.flensburg.marlin.business.api.auth.boundary

import de.lambda9.tailwind.core.KIO
import de.lambda9.tailwind.core.extensions.kio.orDie
import hs.flensburg.marlin.business.App
import hs.flensburg.marlin.business.JEnv
import hs.flensburg.marlin.business.ServiceLayerError
import hs.flensburg.marlin.business.api.auth.control.AuthRepo
import hs.flensburg.marlin.business.api.email.boundary.EmailService
import hs.flensburg.marlin.database.generated.tables.records.LoginBlacklistRecord
import java.time.LocalDateTime

object BlacklistHandler {
    private const val LOGIN_BLACKLIST_DURATION_MINUTES = 15L

    fun addUserToBlacklist(
        userId: Long,
        ipAddress: String?,
        note: String? = null,
        durationMinutes: Long? = LOGIN_BLACKLIST_DURATION_MINUTES,
        sendNotificationEmail: Boolean = true
    ): App<ServiceLayerError, Unit> = KIO.comprehension {
        val now = LocalDateTime.now()

        val (_, env) = !KIO.Companion.access<JEnv>()
        val ipInfo = if (ipAddress != null) {
            IPAddressLookupService.lookUpIpAddressInfo(ipAddress, env.config.ipInfo)
        } else {
            null
        }

        val record = LoginBlacklistRecord().apply {
            this.userId = userId
            this.ipAddress = ipAddress
            this.country = ipInfo?.country
            this.region = ipInfo?.region
            this.city = ipInfo?.city
            this.blockedAt = now
            this.blockedUntil = durationMinutes?.let { now.plusMinutes(it) }
            this.note = note
        }

        !AuthRepo.insertLoginBlacklist(record).orDie()

        if (sendNotificationEmail) !EmailService.sendBlacklistNotificationEmail(userId)

        KIO.Companion.unit
    }

    fun checkUserIsNotBlacklisted(
        userId: Long
    ): App<AuthService.Error, Unit> = KIO.comprehension {
        val blacklist = !AuthRepo.fetchUserLoginBlacklist(userId).orDie()

        if (blacklist != null) {
            val isBlocked = blacklist.blockedUntil == null || blacklist.blockedUntil!!.isAfter(LocalDateTime.now())

            if (isBlocked) {
                !KIO.Companion.fail(AuthService.Error.Blacklisted)
            }
        }

        KIO.Companion.unit
    }

}