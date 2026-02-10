package hs.flensburg.marlin.business.api.subscription.entity

import kotlinx.serialization.Serializable

@Serializable
data class PaymentSheetResponse(
    val paymentIntent: String?,
    val setupIntent: String?,
    val ephemeralKey: String,
    val customerId: String,
    val publishableKey: String
)
