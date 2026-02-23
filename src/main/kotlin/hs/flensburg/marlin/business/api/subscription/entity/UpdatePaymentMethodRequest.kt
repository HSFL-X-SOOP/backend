package hs.flensburg.marlin.business.api.subscription.entity

import kotlinx.serialization.Serializable

@Serializable
data class UpdatePaymentMethodRequest(
    val paymentMethodId: String? = null,
    val setupIntentId: String? = null
)
