package hs.flensburg.marlin.business.api.userDevice.entity

import hs.flensburg.marlin.database.generated.tables.pojos.UserDeviceView
import kotlinx.serialization.Serializable

@Serializable
data class UserDevice(
    var id: Long,
    var fcmToken: String,
    var userId: Long,
) {
    companion object {
        fun from(userDeviceView: UserDeviceView): UserDevice {
            return UserDevice(
                id = userDeviceView.id!!,
                fcmToken = userDeviceView.fcmToken!!,
                userId= userDeviceView.userId!!
            )
        }
    }
}
