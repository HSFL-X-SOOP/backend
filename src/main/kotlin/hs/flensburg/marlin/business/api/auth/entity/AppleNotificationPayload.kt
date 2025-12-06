package hs.flensburg.marlin.business.api.auth.entity

import kotlinx.serialization.Serializable

/**
 * Apple Server-to-Server Notification Payload.
 *
 * @see https://developer.apple.com/documentation/sign_in_with_apple/processing_changes_for_sign_in_with_apple_accounts
 */
@Serializable
data class AppleNotificationPayload(
    val payload: String
)