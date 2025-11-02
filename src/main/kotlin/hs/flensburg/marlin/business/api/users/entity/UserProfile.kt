package hs.flensburg.marlin.business.api.users.entity

import hs.flensburg.marlin.database.generated.enums.Language
import hs.flensburg.marlin.database.generated.enums.MeasurementSystem
import hs.flensburg.marlin.database.generated.enums.UserActivityRole
import hs.flensburg.marlin.database.generated.enums.UserAuthorityRole
import hs.flensburg.marlin.database.generated.tables.pojos.UserView
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toKotlinLocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    var id: Long,
    var email: String,
    var verified: Boolean,
    var authorityRole: UserAuthorityRole,
    var activityRoles: List<UserActivityRole?>,
    var language: Language?,
    var measurementSystem: MeasurementSystem?,
    var userCreatedAt: LocalDateTime,
    var userUpdatedAt: LocalDateTime?,
    var profileCreatedAt: LocalDateTime?,
    var profileUpdatedAt: LocalDateTime?
) {
    companion object {
        fun from(userView: UserView): UserProfile {
            return UserProfile(
                id = userView.id!!,
                email = userView.email!!,
                verified = userView.verified ?: false,
                authorityRole = userView.authorityRole!!,
                activityRoles = userView.activityRoles?.toList() ?: emptyList(),
                language = userView.language,
                measurementSystem = userView.measurementSystem,
                userCreatedAt = userView.userCreatedAt!!.toKotlinLocalDateTime(),
                userUpdatedAt = userView.userUpdatedAt?.toKotlinLocalDateTime(),
                profileCreatedAt = userView.profileCreatedAt?.toKotlinLocalDateTime(),
                profileUpdatedAt = userView.profileUpdatedAt?.toKotlinLocalDateTime(),
            )
        }
    }
}
