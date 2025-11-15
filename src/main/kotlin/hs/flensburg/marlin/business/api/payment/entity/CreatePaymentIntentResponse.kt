package hs.flensburg.marlin.business.api.payment.entity

import kotlinx.serialization.Serializable

@Serializable
data class CreatePaymentIntentResponse(
    val clientSecret: String,
    val paymentIntentId: String,
    val amount: Long,
    val currency: String
)
