package hs.flensburg.marlin.business.api.userDevice.entity

import hs.flensburg.marlin.database.generated.tables.pojos.UserDeviceView
import kotlinx.serialization.Serializable

@Serializable
data class UserDevice(
    var id: Long,
    var deviceId: String,
    var fcmToken: String,
    var userId: Long,
) {
    companion object {
        fun from(userDeviceView: UserDeviceView): UserDevice {
            return UserDevice(
                id = userDeviceView.id!!,
                deviceId = userDeviceView.deviceId!!,
                fcmToken = userDeviceView.fcmToken!!,
                userId= userDeviceView.userId!!
            )
        }
    }
}
