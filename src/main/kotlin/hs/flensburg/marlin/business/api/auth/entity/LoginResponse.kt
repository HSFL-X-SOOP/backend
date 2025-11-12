package hs.flensburg.marlin.business.api.auth.entity

import hs.flensburg.marlin.business.api.users.entity.UserProfile
import kotlinx.serialization.Serializable

@Serializable
data class LoginResponse(
    val accessToken: String,
    val refreshToken: String?,
    val profile: UserProfile?
)