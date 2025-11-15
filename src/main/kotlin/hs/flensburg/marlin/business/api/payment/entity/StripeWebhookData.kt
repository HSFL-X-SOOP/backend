package hs.flensburg.marlin.business.api.payment.entity

import kotlinx.serialization.Serializable

@Serializable
data class StripeWebhookData(
    val intent: PaymentIntentObject
)
