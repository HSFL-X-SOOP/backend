package hs.flensburg.marlin.business.api.subscription.entity

import hs.flensburg.marlin.database.generated.enums.SubscriptionType
import kotlinx.serialization.Serializable

@Serializable
data class ReactivateSubscriptionRequest(
    val subscriptionType: SubscriptionType
)
