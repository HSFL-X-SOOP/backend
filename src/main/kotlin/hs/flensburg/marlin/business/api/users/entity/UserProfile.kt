package hs.flensburg.marlin.business.api.users.entity

import hs.flensburg.marlin.business.api.sensors.entity.raw.LocationDTO
import hs.flensburg.marlin.database.generated.enums.Language
import hs.flensburg.marlin.database.generated.enums.MeasurementSystem
import hs.flensburg.marlin.database.generated.enums.UserActivityRole
import hs.flensburg.marlin.database.generated.enums.UserAuthorityRole
import hs.flensburg.marlin.database.generated.tables.pojos.Location
import hs.flensburg.marlin.database.generated.tables.pojos.UserView
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toKotlinLocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    val id: Long,
    val email: String,
    val verified: Boolean,
    val authorityRole: UserAuthorityRole,
    val activityRoles: List<UserActivityRole?>,
    val firstName: String?,
    val lastName: String?,
    val language: Language?,
    val measurementSystem: MeasurementSystem?,
    val userCreatedAt: LocalDateTime,
    val userUpdatedAt: LocalDateTime?,
    val profileCreatedAt: LocalDateTime?,
    val profileUpdatedAt: LocalDateTime?,
    val assignedLocation: LocationDTO?
) {
    companion object {
        fun from(userView: UserView, location: Location? = null): UserProfile {
            return UserProfile(
                id = userView.id!!,
                email = userView.email!!,
                verified = userView.verified ?: false,
                authorityRole = userView.authorityRole!!,
                activityRoles = userView.activityRoles?.toList() ?: emptyList(),
                firstName = userView.firstName,
                lastName = userView.lastName,
                language = userView.language,
                measurementSystem = userView.measurementSystem,
                userCreatedAt = userView.userCreatedAt!!.toKotlinLocalDateTime(),
                userUpdatedAt = userView.userUpdatedAt?.toKotlinLocalDateTime(),
                profileCreatedAt = userView.profileCreatedAt?.toKotlinLocalDateTime(),
                profileUpdatedAt = userView.profileUpdatedAt?.toKotlinLocalDateTime(),
                assignedLocation = location?.let { LocationDTO.fromLocation(it) }
            )
        }
    }
}
