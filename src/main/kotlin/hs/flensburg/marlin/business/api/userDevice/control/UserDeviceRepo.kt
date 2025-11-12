package hs.flensburg.marlin.business.api.userDevice.control

import de.lambda9.tailwind.jooq.JIO
import de.lambda9.tailwind.jooq.Jooq
import hs.flensburg.marlin.database.generated.enums.Language
import hs.flensburg.marlin.database.generated.enums.MeasurementSystem
import hs.flensburg.marlin.database.generated.enums.UserActivityRole
import hs.flensburg.marlin.database.generated.tables.pojos.UserDeviceView
import hs.flensburg.marlin.database.generated.tables.pojos.User
import hs.flensburg.marlin.database.generated.tables.pojos.UserDevice
import hs.flensburg.marlin.database.generated.tables.pojos.UserView
import hs.flensburg.marlin.database.generated.tables.records.UserDeviceRecord
import hs.flensburg.marlin.database.generated.tables.references.USER
import hs.flensburg.marlin.database.generated.tables.references.USER_DEVICE
import hs.flensburg.marlin.database.generated.tables.references.USER_DEVICE_VIEW
import hs.flensburg.marlin.database.generated.tables.references.USER_PROFILE
import hs.flensburg.marlin.database.generated.tables.references.USER_VIEW

object UserDeviceRepo {
    fun insert(userDevice: UserDeviceRecord): JIO<UserDevice> = Jooq.query {
        insertInto(USER_DEVICE)
            .set(userDevice)
            .returning()
            .fetchInto(UserDevice::class.java).first()
    }

    fun insertDevice(
        userId: Long,
        deviceId: String,
        fcmToken: String
    ): JIO<UserDevice> = Jooq.query {
        insertInto(USER_DEVICE)
            .set(USER_DEVICE.USER_ID, userId)
            .set(USER_DEVICE.DEVICE_ID, deviceId)
            .set(USER_DEVICE.FCM_TOKEN, fcmToken)
            .returning()
            .fetchOneInto(UserDevice::class.java)!!
    }


    fun fetchViewById(id: Long): JIO<UserDeviceView?> = Jooq.query {
        selectFrom(USER_DEVICE_VIEW)
            .where(USER_DEVICE_VIEW.ID.eq(id))
            .fetchOneInto(UserDeviceView::class.java)
    }


    fun fetchById(id: Long): JIO<UserDevice?> = Jooq.query {
        selectFrom(USER_DEVICE)
            .where(USER_DEVICE.ID.eq(id))
            .fetchOneInto(UserDevice::class.java)
    }

    fun fetchAllByUserId(userId: Long): JIO<UserDevice?> = Jooq.query {
        selectFrom(USER_DEVICE)
            .where(USER_DEVICE.USER_ID.eq(userId))
            .fetchOneInto(UserDevice::class.java)
    }

    fun deleteById(id: Long): JIO<Unit> = Jooq.query {
        deleteFrom(USER_DEVICE)
            .where(USER_DEVICE.ID.eq(id))
            .execute()
    }

    fun deleteAllByUserId(id: Long): JIO<Unit> = Jooq.query {
        deleteFrom(USER_DEVICE)
            .where(USER_DEVICE.USER_ID.eq(id))
            .execute()
    }
}
