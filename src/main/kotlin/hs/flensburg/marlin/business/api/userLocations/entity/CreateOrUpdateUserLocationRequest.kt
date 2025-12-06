package hs.flensburg.marlin.business.api.userLocations.entity

import kotlinx.serialization.Serializable

@Serializable
data class CreateOrUpdateUserLocationRequest(
    var userId: Long,
    var locationId: Long,
    var sentHarborNotifications: Boolean
)