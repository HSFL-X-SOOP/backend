package hs.flensburg.marlin.business.api.auth.control

import de.lambda9.tailwind.core.KIO
import de.lambda9.tailwind.core.extensions.kio.orDie
import hs.flensburg.marlin.business.App
import hs.flensburg.marlin.business.ServiceLayerError
import hs.flensburg.marlin.business.api.auth.boundary.IPAddressLookupService
import hs.flensburg.marlin.business.api.email.boundary.EmailService
import hs.flensburg.marlin.database.generated.tables.records.LoginBlacklistRecord
import java.time.LocalDateTime

object BlacklistHandler {
    private const val LOGIN_BLACKLIST_DURATION_MINUTES = 15L

    fun addUserToBlacklist(
        userId: Long,
        ipAddress: String
    ): App<ServiceLayerError, Unit> = KIO.comprehension {
        val now = LocalDateTime.now()
        val ipInfo = IPAddressLookupService.lookUpIpAddressInfo(ipAddress)

        val record = LoginBlacklistRecord().apply {
            this.userId = userId
            this.ipAddress = ipAddress
            this.country = ipInfo.country
            this.region = ipInfo.regionName
            this.city = ipInfo.city
            this.blockedAt = now
            this.blockedUntil = now.plusMinutes(LOGIN_BLACKLIST_DURATION_MINUTES)
        }

        !AuthRepo.insertLoginBlacklist(record).orDie()

        !EmailService.sendBlacklistNotificationEmail(userId)

        KIO.unit
    }

}