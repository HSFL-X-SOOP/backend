package hs.flensburg.marlin.business.api.auth.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AppleNotificationEvent(
    val type: AppleNotificationType,
    val sub: String,
    val email: String? = null,
    @SerialName("is_private_email")
    val isPrivateEmail: Boolean? = null,
    @SerialName("event_time")
    val eventTime: Long? = null
)