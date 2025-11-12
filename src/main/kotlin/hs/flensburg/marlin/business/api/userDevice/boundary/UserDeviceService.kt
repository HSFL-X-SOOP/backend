package hs.flensburg.marlin.business.api.userDevice.boundary

import de.lambda9.tailwind.core.KIO
import de.lambda9.tailwind.core.extensions.kio.onNullFail
import de.lambda9.tailwind.core.extensions.kio.orDie
import hs.flensburg.marlin.business.ApiError
import hs.flensburg.marlin.business.App
import hs.flensburg.marlin.business.ServiceLayerError
import hs.flensburg.marlin.business.api.userDevice.control.UserDeviceRepo
import hs.flensburg.marlin.business.api.userDevice.entity.CreateUserDeviceRequest
import hs.flensburg.marlin.business.api.userDevice.entity.UserDevice
import hs.flensburg.marlin.business.api.users.boundary.UserService
import hs.flensburg.marlin.business.api.users.control.UserRepo
import hs.flensburg.marlin.business.api.users.entity.CreateUserProfileRequest
import hs.flensburg.marlin.business.api.users.entity.UserProfile

object UserDeviceService {
    sealed class Error(private val message: String) : ServiceLayerError {
        object NotFound : Error("User profile not found")
        object BadRequest : Error("Bad request")

        override fun toApiError(): ApiError {
            return when (this) {
                is NotFound -> ApiError.NotFound(message)
                is BadRequest -> ApiError.BadRequest(message)
            }
        }
    }

    /*
    fun getDevices(userId: Long): App<Error, UserDevice> = KIO.comprehension {
        UserDeviceRepo.fetchByUserId(userId).orDie()
    }*/


    fun getUserDevice(userId: Long): App<UserDeviceService.Error, UserDevice> = KIO.comprehension {
        UserDeviceRepo.fetchViewById(userId).orDie().onNullFail { UserDeviceService.Error.NotFound }.map { UserDevice.from(it) }
    }

    fun createDevice(
        userId: Long,
        userDevice: CreateUserDeviceRequest
    ): App<UserDeviceService.Error, UserDevice> = KIO.comprehension {

        !UserDeviceRepo.insertDevice(userId, userDevice.deviceId, userDevice.fcmToken).orDie()

        UserDeviceRepo.fetchViewById(userId).orDie().onNullFail { UserDeviceService.Error.NotFound }.map { UserDevice.from(it) }
    }


    fun deleteUserDevice(id: Long): App<UserDeviceService.Error, Unit> = KIO.comprehension {
        val userDevice = !UserDeviceRepo.fetchById(id).orDie().onNullFail { UserDeviceService.Error.BadRequest }

        UserDeviceRepo.deleteById(userDevice.id!!).orDie()
    }

    /*
    fun deleteAllUserDevice(id: Long): App<UserDeviceService.Error, Unit> = KIO.comprehension {
        val userDevice = !UserDeviceRepo.fetchById(id).orDie().onNullFail { UserDeviceService.Error.BadRequest }

        UserDeviceRepo.deleteById(userDevice.id!!).orDie()
    }
    */
}