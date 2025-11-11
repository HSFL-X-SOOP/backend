package hs.flensburg.marlin.business.api.admin.entity

import hs.flensburg.marlin.database.generated.enums.UserAuthorityRole
import kotlinx.serialization.Serializable

@Serializable
data class ChangeUserRoleRequest(
    val userId: Long,
    val newRole: UserAuthorityRole
)
