package hs.flensburg.marlin.business.api.payment.entity

import kotlinx.serialization.Serializable

@Serializable
data class PaymentIntentObject(
    val id: String,
    val amount: Long,
    val currency: String,
    val status: String,
    val metadata: Map<String, String> = emptyMap()
)
