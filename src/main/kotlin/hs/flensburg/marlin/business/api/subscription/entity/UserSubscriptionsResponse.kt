package hs.flensburg.marlin.business.api.subscription.entity

import kotlinx.serialization.Serializable

@Serializable
data class UserSubscriptionsResponse(
    val notifications: SubscriptionStatusResponse,
    val apiAccess: SubscriptionStatusResponse
)
