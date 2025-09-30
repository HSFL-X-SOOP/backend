package hs.flensburg.marlin.business.api.users.entity

import hs.flensburg.marlin.database.generated.enums.Language
import hs.flensburg.marlin.database.generated.enums.MeasurementSystem
import hs.flensburg.marlin.database.generated.enums.UserActivityRole
import hs.flensburg.marlin.database.generated.tables.pojos.UserProfile
import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@Serializable
data class UserProfileResponse(
    val id: Long,
    val userId: Long,
    val language: Language,
    val roles: List<UserActivityRole>,
    val measurementSystem: MeasurementSystem,
    val createdAt: String,
    val updatedAt: String
) {
    companion object {
        fun from(profile: UserProfile): UserProfileResponse {
            return UserProfileResponse(
                id = profile.id!!,
                userId = profile.userId!!,
                language = profile.language!!,
                roles = profile.role?.filterNotNull() ?: emptyList(),
                measurementSystem = profile.measurementSystem!!,
                createdAt = profile.createdAt.toString(),
                updatedAt = profile.updatedAt.toString()
            )
        }
    }
}
