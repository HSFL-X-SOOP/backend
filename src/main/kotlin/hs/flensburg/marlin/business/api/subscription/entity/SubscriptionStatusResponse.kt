package hs.flensburg.marlin.business.api.subscription.entity

import hs.flensburg.marlin.database.generated.enums.SubscriptionStatus
import hs.flensburg.marlin.database.generated.enums.SubscriptionType
import kotlinx.serialization.Serializable

@Serializable
data class SubscriptionStatusResponse(
    val subscriptionType: SubscriptionType,
    val status: SubscriptionStatus?,
    val currentPeriodEnd: String?,
    val cancelAtPeriodEnd: Boolean,
    val trialEnd: String?
)
