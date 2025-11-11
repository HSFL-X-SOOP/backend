package hs.flensburg.marlin.business.api.users.entity

import hs.flensburg.marlin.database.generated.enums.UserAuthorityRole
import kotlinx.serialization.Serializable

@Serializable
data class UpdateUserRequest(
    val userId: Long,
    val firstName: String?,
    val lastName: String?,
    val authorityRole: UserAuthorityRole,
    val verified: Boolean,
)
