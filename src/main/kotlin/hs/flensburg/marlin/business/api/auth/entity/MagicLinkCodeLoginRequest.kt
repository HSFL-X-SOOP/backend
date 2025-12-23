package hs.flensburg.marlin.business.api.auth.entity

import kotlinx.serialization.Serializable

@Serializable
data class MagicLinkCodeLoginRequest(val email: String, val code: String)
