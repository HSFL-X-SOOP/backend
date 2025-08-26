package hs.flensburg.marlin.business.api.auth.control

import de.lambda9.tailwind.jooq.JIO
import de.lambda9.tailwind.jooq.Jooq
import hs.flensburg.marlin.database.generated.tables.pojos.FailedLoginAttempt
import hs.flensburg.marlin.database.generated.tables.pojos.LoginBlacklist
import hs.flensburg.marlin.database.generated.tables.records.LoginBlacklistRecord
import hs.flensburg.marlin.database.generated.tables.references.FAILED_LOGIN_ATTEMPT
import hs.flensburg.marlin.database.generated.tables.references.LOGIN_BLACKLIST
import java.time.LocalDateTime

object AuthRepo {
    fun insertFailedLoginAttempt(userId: Long, ipAddress: String): JIO<Unit> = Jooq.query {
        insertInto(FAILED_LOGIN_ATTEMPT)
            .set(FAILED_LOGIN_ATTEMPT.USER_ID, userId)
            .set(FAILED_LOGIN_ATTEMPT.IP_ADDRESS, ipAddress)
            .set(FAILED_LOGIN_ATTEMPT.ATTEMPTED_AT, LocalDateTime.now())
            .execute()
    }

    fun insertLoginBlacklist(
        record: LoginBlacklistRecord
    ): JIO<Unit> = Jooq.query {
        insertInto(LOGIN_BLACKLIST)
            .set(record)
            .execute()
    }

    fun fetchFailedLoginAttempts(userId: Long): JIO<List<FailedLoginAttempt>> = Jooq.query {
        selectFrom(FAILED_LOGIN_ATTEMPT)
            .where(FAILED_LOGIN_ATTEMPT.USER_ID.eq(userId))
            .fetchInto(FailedLoginAttempt::class.java)
    }

    fun fetchUserLoginBlacklist(userId: Long): JIO<LoginBlacklist?> = Jooq.query {
        selectFrom(LOGIN_BLACKLIST)
            .where(LOGIN_BLACKLIST.USER_ID.eq(userId))
            .orderBy(LOGIN_BLACKLIST.BLOCKED_UNTIL.desc())
            .limit(1)
            .fetchOneInto(LoginBlacklist::class.java)
    }

    fun deleteExpiredFailedLoginAttempts(cutoff: LocalDateTime): JIO<Unit> = Jooq.query {
        deleteFrom(FAILED_LOGIN_ATTEMPT)
            .where(FAILED_LOGIN_ATTEMPT.ATTEMPTED_AT.lessOrEqual(cutoff))
            .execute()
    }
}