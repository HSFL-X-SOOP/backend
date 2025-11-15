package hs.flensburg.marlin.business.api.payment.entity

import hs.flensburg.marlin.database.generated.enums.PaymentStatus
import kotlinx.serialization.Serializable

@Serializable
data class PaymentIntentData(
    val id: String,
    val amount: Long,
    val currency: String,
    val status: PaymentStatus,
    val metadata: Map<String, String>,
    val eventType: String
)
