package hs.flensburg.marlin.business.schedulerJobs.auth

import de.lambda9.tailwind.core.KIO
import de.lambda9.tailwind.jooq.JIO
import hs.flensburg.marlin.business.api.auth.control.AuthRepo
import java.time.LocalDateTime

object AuthSchedulerService {

    private const val EXPIRED_TOKEN_CLEANUP_MINUTES = 15L

    fun deleteExpiredRestrictionLogs(now: LocalDateTime): JIO<Unit> = KIO.comprehension {
        val cutoff = now.minusMinutes(EXPIRED_TOKEN_CLEANUP_MINUTES)

        AuthRepo.deleteExpiredFailedLoginAttempts(cutoff)
    }
}