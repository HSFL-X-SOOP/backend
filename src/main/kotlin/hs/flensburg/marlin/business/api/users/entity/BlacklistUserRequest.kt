package hs.flensburg.marlin.business.api.users.entity

import kotlinx.serialization.Serializable

@Serializable
data class BlacklistUserRequest(
    val userId: Long,
    val reason: String,
    val blockUntil: Long?
)
