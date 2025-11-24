package hs.flensburg.marlin.business.api.userDevice.entity

import kotlinx.serialization.Serializable

@Serializable
data class CreateUserDeviceRequest(
    val fcmToken: String,
    val userId: Long
) {
    /*
    init {
        require(roles.isNotEmpty()) { "At least one role must be specified" }
    }
    */
}
