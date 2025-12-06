package hs.flensburg.marlin.business.api.auth.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class AppleNotificationType {
    @SerialName("consent-revoked")
    CONSENT_REVOKED,

    @SerialName("account-delete")
    ACCOUNT_DELETE,

    @SerialName("email-disabled")
    EMAIL_DISABLED,

    @SerialName("email-enabled")
    EMAIL_ENABLED
}
