package hs.flensburg.marlin.business.api.payment.entity

import kotlinx.serialization.Serializable

@Serializable
data class CreatePaymentIntentRequest(
    val amount: Long,
    val currency: String
)
