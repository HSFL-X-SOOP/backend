package hs.flensburg.marlin.business.api.users.control

import de.lambda9.tailwind.jooq.JIO
import de.lambda9.tailwind.jooq.Jooq
import hs.flensburg.marlin.database.generated.enums.Language
import hs.flensburg.marlin.database.generated.enums.MeasurementSystem
import hs.flensburg.marlin.database.generated.enums.UserActivityRole
import hs.flensburg.marlin.database.generated.tables.pojos.User
import hs.flensburg.marlin.database.generated.tables.pojos.UserProfile
import hs.flensburg.marlin.database.generated.tables.records.UserRecord
import hs.flensburg.marlin.database.generated.tables.references.USER
import hs.flensburg.marlin.database.generated.tables.references.USER_PROFILE

object UserRepo {
    fun insert(user: UserRecord): JIO<User> = Jooq.query {
        insertInto(USER)
            .set(user)
            .returning()
            .fetchInto(User::class.java).first()
    }

    fun fetchById(id: Long): JIO<User?> = Jooq.query {
        selectFrom(USER)
            .where(USER.ID.eq(id))
            .fetchOneInto(User::class.java)
    }

    fun setEmailIsVerified(id: Long): JIO<Unit> = Jooq.query {
        update(USER)
            .set(USER.VERIFIED, true)
            .where(USER.ID.eq(id))
            .execute()
    }

    fun fetchByEmail(email: String): JIO<User?> = Jooq.query {
        selectFrom(USER)
            .where(USER.EMAIL.eq(email))
            .fetchOneInto(User::class.java)
    }

    fun fetchProfileByUserId(userId: Long): JIO<UserProfile?> = Jooq.query {
        selectFrom(USER_PROFILE)
            .where(USER_PROFILE.USER_ID.eq(userId))
            .fetchOneInto(UserProfile::class.java)
    }

    fun insertProfile(
        userId: Long,
        roles: List<UserActivityRole>,
        language: Language?,
        measurementSystem: MeasurementSystem?
    ): JIO<UserProfile> = Jooq.query {
        insertInto(USER_PROFILE)
            .set(USER_PROFILE.USER_ID, userId)
            .set(USER_PROFILE.LANGUAGE, language ?: Language.EN)
            .set(USER_PROFILE.ROLE, roles.toTypedArray())
            .set(USER_PROFILE.MEASUREMENT_SYSTEM, measurementSystem ?: MeasurementSystem.METRIC)
            .returning()
            .fetchOneInto(UserProfile::class.java)!!
    }

    fun updateProfile(
        userId: Long,
        language: Language?,
        roles: List<UserActivityRole>?,
        measurementSystem: MeasurementSystem?
    ): JIO<UserProfile?> = Jooq.query {
        update(USER_PROFILE)
            .set(USER_PROFILE.LANGUAGE, language)
            .set(USER_PROFILE.ROLE, roles?.toTypedArray())
            .set(USER_PROFILE.MEASUREMENT_SYSTEM, measurementSystem)
            .where(USER_PROFILE.USER_ID.eq(userId))
            .returning()
            .fetchOneInto(UserProfile::class.java)
    }
}