package hs.flensburg.marlin.business.api.auth.entity

import kotlinx.serialization.Serializable

@Serializable
data class LoginResponse(val accessToken: String, val refreshToken: String?)