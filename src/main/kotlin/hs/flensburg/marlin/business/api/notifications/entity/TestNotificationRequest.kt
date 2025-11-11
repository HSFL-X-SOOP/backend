package hs.flensburg.marlin.business.api.notifications.entity

import kotlinx.serialization.Serializable

@Serializable
data class TestNotificationRequest(
    val FCMtoken: String,
    val title: String,
    val message: String,
)