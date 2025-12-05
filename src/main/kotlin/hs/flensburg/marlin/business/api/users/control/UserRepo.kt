package hs.flensburg.marlin.business.api.users.control

import de.lambda9.tailwind.jooq.JIO
import de.lambda9.tailwind.jooq.Jooq
import hs.flensburg.marlin.business.Page
import hs.flensburg.marlin.business.PageResult
import hs.flensburg.marlin.business.api.users.entity.UserSearchParameters
import hs.flensburg.marlin.business.api.users.entity.UserProfile
import hs.flensburg.marlin.business.setIfNotNull
import hs.flensburg.marlin.business.setWhen
import hs.flensburg.marlin.database.generated.enums.Language
import hs.flensburg.marlin.database.generated.enums.MeasurementSystem
import hs.flensburg.marlin.database.generated.enums.UserActivityRole
import hs.flensburg.marlin.database.generated.enums.UserAuthorityRole
import hs.flensburg.marlin.database.generated.tables.pojos.FailedLoginAttempt
import hs.flensburg.marlin.database.generated.tables.pojos.LoginBlacklist
import hs.flensburg.marlin.database.generated.tables.pojos.User
import hs.flensburg.marlin.database.generated.tables.pojos.UserView
import hs.flensburg.marlin.database.generated.tables.records.UserRecord
import hs.flensburg.marlin.database.generated.tables.references.FAILED_LOGIN_ATTEMPT
import hs.flensburg.marlin.database.generated.tables.references.HARBOR_MASTER_LOCATION
import hs.flensburg.marlin.database.generated.tables.references.LOGIN_BLACKLIST
import hs.flensburg.marlin.database.generated.tables.references.USER
import hs.flensburg.marlin.database.generated.tables.references.USER_PROFILE
import hs.flensburg.marlin.database.generated.tables.references.USER_VIEW

object UserRepo {
    fun insert(user: UserRecord): JIO<User> = Jooq.query {
        insertInto(USER)
            .set(user)
            .returning()
            .fetchInto(User::class.java).first()
    }

    fun fetch(page: Page<UserSearchParameters>): JIO<PageResult<UserProfile>> = Jooq.query {
        val user = selectFrom(USER_VIEW)
            .where(page.parameterWrapper.toCondition())
            .orderBy(page.order.toOrderField())
            .limit(page.limit)
            .offset(page.offset)
            .fetchInto(UserView::class.java)
            .map { UserProfile.from(it) }

        val count = selectCount()
            .from(USER_VIEW)
            .where(page.parameterWrapper.toCondition())
            .fetchOne(0, Long::class.java)!!

        val totalCount = selectCount()
            .from(USER_VIEW)
            .fetchOne(0, Long::class.java)!!

        PageResult(
            items = user,
            filteredCount = count,
            totalCount = totalCount
        )
    }

    fun fetchById(id: Long): JIO<User?> = Jooq.query {
        selectFrom(USER)
            .where(USER.ID.eq(id))
            .fetchOneInto(User::class.java)
    }

    fun fetchViewById(id: Long): JIO<UserView?> = Jooq.query {
        selectFrom(USER_VIEW)
            .where(USER_VIEW.ID.eq(id))
            .fetchOneInto(UserView::class.java)
    }

    fun fetchByEmail(email: String): JIO<User?> = Jooq.query {
        selectFrom(USER)
            .where(USER.EMAIL.eq(email))
            .fetchOneInto(User::class.java)
    }

    fun fetchViewByEmail(email: String): JIO<UserView?> = Jooq.query {
        selectFrom(USER_VIEW)
            .where(USER_VIEW.EMAIL.eq(email))
            .fetchOneInto(UserView::class.java)
    }

    fun fetchRecentActivity(userId: Long): JIO<PageResult<String>> = Jooq.query {
        val blacklistEntries = select(LOGIN_BLACKLIST)
            .where(LOGIN_BLACKLIST.USER_ID.eq(userId))
            .fetchInto(LoginBlacklist::class.java)

        val failedLoginAttempts = select(FAILED_LOGIN_ATTEMPT)
            .where(FAILED_LOGIN_ATTEMPT.USER_ID.eq(userId))
            .fetchInto(FailedLoginAttempt::class.java)

        val activities = (blacklistEntries.map {
            "User was blacklisted on ${it.blockedAt} until ${it.blockedUntil ?: "indefinitely"}. Note: ${it.note ?: "No note"}"
        } + failedLoginAttempts.map {
            "Failed login attempt on ${it.attemptedAt} from IP ${it.ipAddress ?: "unknown"}"
        }).sortedByDescending {
            val regex = Regex("""on (.+?) from|on (.+?) until""")
            val matchResult = regex.find(it)
            val dateString = matchResult?.groups?.get(1)?.value ?: matchResult?.groups?.get(2)?.value
            dateString?.let { ds -> java.time.LocalDateTime.parse(ds) } ?: java.time.LocalDateTime.MIN
        }

        val count = selectCount()
            .from(USER_PROFILE)
            .where(USER_PROFILE.USER_ID.eq(userId))
            .fetchOne(0, Long::class.java)!!

        PageResult(
            items = activities,
            filteredCount = count,
            totalCount = count
        )
    }

    fun countAllUsers(): JIO<Int> = Jooq.query {
        selectCount()
            .from(USER)
            .fetchOneInto(Int::class.java) ?: 0
    }

    fun insertProfile(
        userId: Long,
        roles: List<UserActivityRole>,
        language: Language?,
        measurementSystem: MeasurementSystem?
    ): JIO<hs.flensburg.marlin.database.generated.tables.pojos.UserProfile> = Jooq.query {
        insertInto(USER_PROFILE)
            .set(USER_PROFILE.USER_ID, userId)
            .set(USER_PROFILE.LANGUAGE, language ?: Language.EN)
            .set(USER_PROFILE.ROLE, roles.toTypedArray())
            .set(USER_PROFILE.MEASUREMENT_SYSTEM, measurementSystem ?: MeasurementSystem.METRIC)
            .returning()
            .fetchOneInto(hs.flensburg.marlin.database.generated.tables.pojos.UserProfile::class.java)!!
    }

    fun update(
        userId: Long,
        firstName: String?,
        lastName: String?,
        authorityRole: UserAuthorityRole,
        verified: Boolean
    ): JIO<User?> = Jooq.query {
        update(USER)
            .set(USER.ROLE, authorityRole)
            .set(USER.VERIFIED, verified)
            .setWhen(USER.FIRST_NAME, firstName) { !firstName.isNullOrBlank() }
            .setWhen(USER.LAST_NAME, lastName) { !lastName.isNullOrBlank() }
            .where(USER.ID.eq(userId))
            .returning()
            .fetchOneInto(User::class.java)
    }

    fun updateProfile(
        userId: Long,
        firstName: String?,
        lastName: String?,
        language: Language?,
        roles: List<UserActivityRole>?,
        measurementSystem: MeasurementSystem?
    ): JIO<hs.flensburg.marlin.database.generated.tables.pojos.UserProfile?> = Jooq.query {
        update(USER)
            .setWhen(USER.FIRST_NAME, firstName) { !firstName.isNullOrBlank() }
            .setWhen(USER.LAST_NAME, lastName) { !lastName.isNullOrBlank() }
            .where(USER.ID.eq(userId))
            .execute()

        update(USER_PROFILE)
            .setIfNotNull(USER_PROFILE.LANGUAGE, language)
            .setIfNotNull(USER_PROFILE.ROLE, roles?.toTypedArray())
            .setIfNotNull(USER_PROFILE.MEASUREMENT_SYSTEM, measurementSystem)
            .where(USER_PROFILE.USER_ID.eq(userId))
            .returning()
            .fetchOneInto(hs.flensburg.marlin.database.generated.tables.pojos.UserProfile::class.java)
    }

    fun setEmailIsVerified(id: Long): JIO<Unit> = Jooq.query {
        update(USER)
            .set(USER.VERIFIED, true)
            .where(USER.ID.eq(id))
            .execute()
    }

    fun deleteById(id: Long): JIO<Unit> = Jooq.query {
        deleteFrom(USER)
            .where(USER.ID.eq(id))
            .execute()
    }

    fun fetchUserAssignedLocationId(userId: Long): JIO<Long> = Jooq.query {
        val harborMasterLocation = selectFrom(HARBOR_MASTER_LOCATION)
            .where(HARBOR_MASTER_LOCATION.USER_ID.eq(userId))
            .fetchOneInto(hs.flensburg.marlin.database.generated.tables.pojos.HarborMasterLocation::class.java)!!

        harborMasterLocation.locationId!!
    }

}