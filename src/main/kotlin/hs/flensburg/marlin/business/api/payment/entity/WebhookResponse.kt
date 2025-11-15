package hs.flensburg.marlin.business.api.payment.entity

import kotlinx.serialization.Serializable

@Serializable
data class WebhookResponse(
    val received: Boolean,
    val paymentId: String
)
