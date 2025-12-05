package hs.flensburg.marlin.business.api.auth.entity

import kotlinx.serialization.Serializable

@Serializable
data class AppleLoginRequest(
    val identityToken: String,
    val user: String? = null,
    val email: String? = null,
    val givenName: String? = null,
    val familyName: String? = null
)