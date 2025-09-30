package hs.flensburg.marlin.business.api.users.entity

import hs.flensburg.marlin.database.generated.enums.Language
import hs.flensburg.marlin.database.generated.enums.MeasurementSystem
import hs.flensburg.marlin.database.generated.enums.UserActivityRole
import kotlinx.serialization.Serializable

@Serializable
data class UpdateUserProfileRequest(
    val language: Language? = null,
    val roles: List<UserActivityRole>? = null,
    val measurementSystem: MeasurementSystem? = null
)