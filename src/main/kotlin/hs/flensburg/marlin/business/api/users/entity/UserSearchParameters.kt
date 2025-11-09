package hs.flensburg.marlin.business.api.users.entity

import hs.flensburg.marlin.business.Conditional
import hs.flensburg.marlin.business.ConditionalFactory
import hs.flensburg.marlin.database.generated.enums.Language
import hs.flensburg.marlin.database.generated.enums.MeasurementSystem
import hs.flensburg.marlin.database.generated.enums.UserActivityRole
import hs.flensburg.marlin.database.generated.enums.UserAuthorityRole
import hs.flensburg.marlin.database.generated.tables.references.USER_VIEW
import io.ktor.http.Parameters
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toJavaLocalDateTime
import org.jooq.Condition

data class UserSearchParameters(
    var id: Long?,
    var email: String?,
    var verified: Boolean?,
    var authorityRole: UserAuthorityRole?,
    var activityRoles: List<UserActivityRole?>,
    var language: Language?,
    var measurementSystem: MeasurementSystem?,
    var userCreatedAt: LocalDateTime?,
    var userUpdatedAt: LocalDateTime?,
    var profileCreatedAt: LocalDateTime?,
    var profileUpdatedAt: LocalDateTime?
) : Conditional {
    override fun toCondition(): Condition {
        return listOfNotNull(
            id?.let { USER_VIEW.ID.eq(it) },
            email?.let { USER_VIEW.EMAIL.likeIgnoreCase(it) },
            verified?.let { USER_VIEW.VERIFIED.eq(it) },
            authorityRole?.let { USER_VIEW.AUTHORITY_ROLE.eq(it) },
            if (activityRoles.isNotEmpty()) USER_VIEW.ACTIVITY_ROLES.contains(activityRoles.toTypedArray()) else null,
            language?.let { USER_VIEW.LANGUAGE.eq(it) },
            measurementSystem?.let { USER_VIEW.MEASUREMENT_SYSTEM.eq(it) },
            userCreatedAt?.let { USER_VIEW.USER_CREATED_AT.eq(it.toJavaLocalDateTime()) },
            userUpdatedAt?.let { USER_VIEW.USER_UPDATED_AT.eq(it.toJavaLocalDateTime()) },
            profileCreatedAt?.let { USER_VIEW.PROFILE_CREATED_AT.eq(it.toJavaLocalDateTime()) },
            profileUpdatedAt?.let { USER_VIEW.PROFILE_UPDATED_AT.eq(it.toJavaLocalDateTime()) },
        ).reduceOrNull { acc, condition -> acc.and(condition) } ?: org.jooq.impl.DSL.noCondition()
    }

    companion object : ConditionalFactory<UserSearchParameters> {
        override fun from(queryParams: Parameters): UserSearchParameters {
            return UserSearchParameters(
                id = queryParams["id"]?.toLongOrNull(),
                email = queryParams["email"],
                verified = queryParams["verified"]?.toBooleanStrictOrNull(),
                authorityRole = queryParams["authorityRole"]?.let { UserAuthorityRole.valueOf(it.uppercase()) },
                activityRoles = queryParams.getAll("activityRoles")?.map { UserActivityRole.valueOf(it.uppercase()) } ?: emptyList(),
                language = queryParams["language"]?.let { Language.valueOf(it) },
                measurementSystem = queryParams["measurementSystem"]?.let { MeasurementSystem.valueOf(it) },
                userCreatedAt = queryParams["userCreatedAt"]?.let { LocalDateTime.parse(it) },
                userUpdatedAt = queryParams["userUpdatedAt"]?.let { LocalDateTime.parse(it) },
                profileCreatedAt = queryParams["profileCreatedAt"]?.let { LocalDateTime.parse(it) },
                profileUpdatedAt = queryParams["profileUpdatedAt"]?.let { LocalDateTime.parse(it) },
            )
        }
    }
}
