package hs.flensburg.marlin.business.api.payment.entity

import kotlinx.serialization.Serializable

@Serializable
data class StripeWebhookEvent(
    val id: String,
    val type: String,
    val data: StripeWebhookData
)
