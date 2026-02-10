package hs.flensburg.marlin.business.api.subscription.entity

import hs.flensburg.marlin.database.generated.enums.SubscriptionType
import kotlinx.serialization.Serializable

@Serializable
data class PauseSubscriptionRequest(
    val subscriptionType: SubscriptionType
)
