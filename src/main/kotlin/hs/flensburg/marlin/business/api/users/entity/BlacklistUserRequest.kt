package hs.flensburg.marlin.business.api.users.entity

data class BlacklistUserRequest(
    val userId: Long,
    val reason: String,
    val blockUntil: Long?
)
