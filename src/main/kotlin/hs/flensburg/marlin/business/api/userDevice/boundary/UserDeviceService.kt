package hs.flensburg.marlin.business.api.userDevice.boundary

import de.lambda9.tailwind.core.KIO
import de.lambda9.tailwind.core.extensions.kio.onNullFail
import de.lambda9.tailwind.core.extensions.kio.orDie
import hs.flensburg.marlin.business.ApiError
import hs.flensburg.marlin.business.App
import hs.flensburg.marlin.business.JEnv
import hs.flensburg.marlin.business.ServiceLayerError
import hs.flensburg.marlin.business.api.userDevice.control.UserDeviceRepo
import hs.flensburg.marlin.business.api.userDevice.entity.CreateUserDeviceRequest
import hs.flensburg.marlin.business.api.userDevice.entity.UserDevice

object UserDeviceService {
    sealed class Error(private val message: String) : ServiceLayerError {
        object NotFound : Error("User device not found")
        object BadRequest : Error("Bad request")

        override fun toApiError(): ApiError {
            return when (this) {
                is NotFound -> ApiError.NotFound(message)
                is BadRequest -> ApiError.BadRequest(message)
            }
        }
    }

    fun getUserDevice(userId: Long): App<UserDeviceService.Error, UserDevice> = KIO.comprehension {
        UserDeviceRepo.fetchViewById(userId).orDie().onNullFail { UserDeviceService.Error.NotFound }.map { UserDevice.from(it) }
    }

    fun getAllUserDevices(userId: Long): App<UserDeviceService.Error, List<UserDevice>> = KIO.comprehension {
        UserDeviceRepo.fetchAllByUserId(userId).orDie().onNullFail { Error.NotFound } as KIO<JEnv, Error, List<UserDevice>>
    }

    fun createDevice(
        userId: Long,
        userDevice: CreateUserDeviceRequest
    ): App<UserDeviceService.Error, UserDevice> = KIO.comprehension {

        !UserDeviceRepo.insertDevice(userId, userDevice.fcmToken).orDie()

        UserDeviceRepo.fetchViewById(userId).orDie().onNullFail { UserDeviceService.Error.NotFound }.map { UserDevice.from(it) }
    }


    fun deleteUserDevice(id: Long): App<UserDeviceService.Error, Unit> = KIO.comprehension {
        val userDevice = !UserDeviceRepo.fetchById(id).orDie().onNullFail { UserDeviceService.Error.BadRequest }

        UserDeviceRepo.deleteById(userDevice.id!!).orDie()
    }
}