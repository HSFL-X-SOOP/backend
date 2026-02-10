package hs.flensburg.marlin.business.api.apikey.control

import de.lambda9.tailwind.jooq.JIO
import de.lambda9.tailwind.jooq.Jooq
import hs.flensburg.marlin.database.generated.tables.pojos.ApiKey
import hs.flensburg.marlin.database.generated.tables.references.API_KEY
import org.jooq.impl.DSL
import java.util.UUID

object ApiKeyRepository {

    fun insert(userId: Long, keyPrefix: String, keyHash: String, name: String?): JIO<ApiKey> = Jooq.query {
        insertInto(API_KEY)
            .set(API_KEY.USER_ID, userId)
            .set(API_KEY.KEY_PREFIX, keyPrefix)
            .set(API_KEY.KEY_HASH, keyHash)
            .set(API_KEY.NAME, name)
            .returning()
            .fetchOneInto(ApiKey::class.java)!!
    }

    fun findActiveByUserId(userId: Long): JIO<ApiKey?> = Jooq.query {
        selectFrom(API_KEY)
            .where(
                API_KEY.USER_ID.eq(userId)
                    .and(API_KEY.IS_ACTIVE.isTrue)
            )
            .fetchOneInto(ApiKey::class.java)
    }

    fun findAllByUserId(userId: Long): JIO<List<ApiKey>> = Jooq.query {
        selectFrom(API_KEY)
            .where(API_KEY.USER_ID.eq(userId))
            .orderBy(API_KEY.CREATED_AT.desc())
            .fetchInto(ApiKey::class.java)
    }

    fun findActiveByPrefix(prefix: String): JIO<List<ApiKey>> = Jooq.query {
        selectFrom(API_KEY)
            .where(
                API_KEY.KEY_PREFIX.eq(prefix)
                    .and(API_KEY.IS_ACTIVE.isTrue)
            )
            .fetchInto(ApiKey::class.java)
    }

    fun revokeAllForUser(userId: Long): JIO<Unit> = Jooq.query {
        update(API_KEY)
            .set(API_KEY.IS_ACTIVE, false)
            .set(API_KEY.REVOKED_AT, DSL.currentOffsetDateTime())
            .where(
                API_KEY.USER_ID.eq(userId)
                    .and(API_KEY.IS_ACTIVE.isTrue)
            )
            .execute()
    }

    fun revokeById(keyId: UUID): JIO<Boolean> = Jooq.query {
        val updated = update(API_KEY)
            .set(API_KEY.IS_ACTIVE, false)
            .set(API_KEY.REVOKED_AT, DSL.currentOffsetDateTime())
            .where(
                API_KEY.ID.eq(keyId)
                    .and(API_KEY.IS_ACTIVE.isTrue)
            )
            .execute()
        updated > 0
    }

    fun updateLastUsed(keyId: UUID): JIO<Unit> = Jooq.query {
        update(API_KEY)
            .set(API_KEY.LAST_USED_AT, DSL.currentOffsetDateTime())
            .where(API_KEY.ID.eq(keyId))
            .execute()
    }
}
