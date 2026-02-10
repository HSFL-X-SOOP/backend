package hs.flensburg.marlin.business.api.subscription.entity

import kotlinx.serialization.Serializable

@Serializable
data class CreatePortalSessionRequest(
    val returnUrl: String
)
