package hs.flensburg.marlin.business.api.users.entity

import hs.flensburg.marlin.database.generated.enums.Language
import hs.flensburg.marlin.database.generated.enums.MeasurementSystem
import hs.flensburg.marlin.database.generated.enums.UserActivityRole
import kotlinx.serialization.Serializable

@Serializable
data class CreateUserProfileRequest(
    val roles: List<UserActivityRole>,
    val firstName: String?,
    val lastName: String?,
    val language: Language,
    val measurementSystem: MeasurementSystem
) {
    init {
        require(roles.isNotEmpty()) { "At least one role must be specified" }
    }
}
