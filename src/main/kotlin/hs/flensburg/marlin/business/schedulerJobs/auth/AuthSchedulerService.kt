package hs.flensburg.marlin.business.schedulerJobs.auth

import de.lambda9.tailwind.core.KIO
import de.lambda9.tailwind.jooq.JIO
import hs.flensburg.marlin.business.api.auth.control.AuthRepo
import hs.flensburg.marlin.business.api.auth.control.MagicLinkCodeRepo
import java.time.LocalDateTime

object AuthSchedulerService {
    private const val EXPIRED_TOKEN_CLEANUP_MINUTES = 15L
    private const val MAGIC_LINK_CODE_TTL_MINUTES = 30L

    fun deleteExpiredRestrictionLogs(now: LocalDateTime): JIO<Unit> = KIO.comprehension {
        val cutoff = now.minusMinutes(EXPIRED_TOKEN_CLEANUP_MINUTES)

        AuthRepo.deleteExpiredFailedLoginAttempts(cutoff)
    }

    fun deleteExpiredMagicLinkCodes(now: LocalDateTime): JIO<Unit> = KIO.comprehension {
        val cutoff = now.minusMinutes(MAGIC_LINK_CODE_TTL_MINUTES)

        MagicLinkCodeRepo.deleteExpiredCodes(cutoff)
    }
}