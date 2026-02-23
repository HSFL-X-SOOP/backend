package hs.flensburg.marlin.business.api.subscription.entity

import kotlinx.serialization.Serializable

@Serializable
data class SetupIntentResponse(
    val clientSecret: String,
    val customerId: String,
    val ephemeralKey: String
)
