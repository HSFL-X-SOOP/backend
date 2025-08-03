package hs.flensburg.marlin.business.api.email.control

import de.lambda9.tailwind.jooq.JIO
import de.lambda9.tailwind.jooq.Jooq
import hs.flensburg.marlin.database.generated.enums.EmailType
import hs.flensburg.marlin.database.generated.tables.pojos.Email
import hs.flensburg.marlin.database.generated.tables.records.EmailRecord
import hs.flensburg.marlin.database.generated.tables.references.EMAIL
import java.time.LocalDateTime

object EmailRepo {
    fun insert(email: EmailRecord): JIO<Email> = Jooq.query {
        insertInto(EMAIL)
            .set(email)
            .returning()
            .fetchOneInto(Email::class.java)!!
    }

    fun fetchLastByUserAndType(userId: Long, type: EmailType): JIO<Email?> = Jooq.query {
        selectFrom(EMAIL)
            .where(EMAIL.USER_ID.eq(userId).and(EMAIL.TYPE.eq(type)))
            .orderBy(EMAIL.SENT_AT.desc())
            .limit(1)
            .fetchOneInto(Email::class.java)
    }

    fun fetchPending(): JIO<List<Email>> = Jooq.query {
        selectFrom(EMAIL)
            .where(EMAIL.SENT_AT.isNull)
            .fetchInto(Email::class.java)
    }

    fun updateSentAt(id: Long): JIO<Unit> = Jooq.query {
        update(EMAIL)
            .set(EMAIL.SENT_AT, LocalDateTime.now())
            .where(EMAIL.ID.eq(id))
            .execute()
    }

    fun setError(id: Long, error: String): JIO<Unit> = Jooq.query {
        update(EMAIL)
            .set(EMAIL.ERROR, error)
            .where(EMAIL.ID.eq(id))
            .execute()
    }
}