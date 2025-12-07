package hs.flensburg.marlin.business.api.notificationLocation.entity

import kotlinx.serialization.Serializable

@Serializable
data class CreateOrUpdateNotificationLocationRequest(
    var locationId: Long,
    var notificationTitle: String,
    var notificationText: String,
    var createdBy: Long
)