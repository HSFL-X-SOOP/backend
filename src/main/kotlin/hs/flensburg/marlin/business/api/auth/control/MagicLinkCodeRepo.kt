package hs.flensburg.marlin.business.api.auth.control

import de.lambda9.tailwind.jooq.JIO
import de.lambda9.tailwind.jooq.Jooq
import hs.flensburg.marlin.database.generated.tables.pojos.MagicLinkCode
import hs.flensburg.marlin.database.generated.tables.records.MagicLinkCodeRecord
import hs.flensburg.marlin.database.generated.tables.references.MAGIC_LINK_CODE
import java.time.LocalDateTime

object MagicLinkCodeRepo {
    private const val CODE_TTL_MINUTES = 30L

    fun insert(record: MagicLinkCodeRecord): JIO<MagicLinkCodeRecord> = Jooq.query {
        insertInto(MAGIC_LINK_CODE)
            .set(record)
            .returning()
            .fetchOne()!!
    }

    fun fetchValidByCode(userId: Long, code: String): JIO<MagicLinkCode?> = Jooq.query {
        val cutoff = LocalDateTime.now().minusMinutes(CODE_TTL_MINUTES)
        selectFrom(MAGIC_LINK_CODE)
            .where(MAGIC_LINK_CODE.USER_ID.eq(userId))
            .and(MAGIC_LINK_CODE.CODE.eq(code))
            .and(MAGIC_LINK_CODE.CREATED_AT.greaterThan(cutoff))
            .and(MAGIC_LINK_CODE.USED_AT.isNull)
            .orderBy(MAGIC_LINK_CODE.CREATED_AT.desc())
            .limit(1)
            .fetchOneInto(MagicLinkCode::class.java)
    }

    fun markAsUsed(id: Long): JIO<Unit> = Jooq.query {
        update(MAGIC_LINK_CODE)
            .set(MAGIC_LINK_CODE.USED_AT, LocalDateTime.now())
            .where(MAGIC_LINK_CODE.ID.eq(id))
            .execute()
    }

    fun deleteExpiredCodes(): JIO<Int> = Jooq.query {
        val cutoff = LocalDateTime.now().minusMinutes(CODE_TTL_MINUTES)
        deleteFrom(MAGIC_LINK_CODE)
            .where(MAGIC_LINK_CODE.CREATED_AT.lessThan(cutoff))
            .execute()
    }

    fun generateCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..6).map { chars.random() }.joinToString("")
    }
}
