package hs.flensburg.marlin.business.api.admin.boundary

import de.lambda9.tailwind.core.KIO
import de.lambda9.tailwind.core.extensions.kio.onNullFail
import de.lambda9.tailwind.core.extensions.kio.orDie
import hs.flensburg.marlin.business.ApiError
import hs.flensburg.marlin.business.ServiceLayerError
import hs.flensburg.marlin.business.api.admin.entity.DashboardInfo
import hs.flensburg.marlin.business.App
import hs.flensburg.marlin.business.api.location.control.LocationRepo
import hs.flensburg.marlin.business.api.sensors.control.SensorRepo
import hs.flensburg.marlin.business.api.users.control.UserRepo
import hs.flensburg.marlin.database.generated.enums.UserAuthorityRole

object AdminService {
    sealed class Error(private val message: String) : ServiceLayerError {
        object NotFound : Error("Requested data not found")
        object BadRequest : Error("Bad request")
        object UserNotHarborMaster : Error("User is not a harbor master")
        object LocationNotFound : Error("Location not found")

        override fun toApiError(): ApiError {
            return when (this) {
                is NotFound -> ApiError.NotFound(message)
                is BadRequest -> ApiError.BadRequest(message)
                is UserNotHarborMaster -> ApiError.BadRequest(message)
                is LocationNotFound -> ApiError.NotFound(message)
            }
        }
    }

    fun getDashboardInformation(): App<Error, DashboardInfo> = KIO.comprehension {

        val totalSensors = !SensorRepo.countAllActiveSensors().orDie()
        val totalMeasurement = !SensorRepo.countAllMeasurementsToday().orDie()
        val totalLocations = !LocationRepo.countAllLocations().orDie()
        val totalUsers = !UserRepo.countAllUsers().orDie()

        KIO.ok(
            DashboardInfo(
                totalLocations = totalLocations,
                totalSensors = totalSensors,
                totalMeasurements = totalMeasurement,
                totalUsers = totalUsers
            )
        )

    }

    fun assignLocationToHarborMaster(
        userId: Long,
        locationId: Long,
        adminId: Long
    ): App<Error, Unit> = KIO.comprehension {
        val user = !UserRepo.fetchById(userId).orDie().onNullFail { Error.NotFound }
        !KIO.failOn(user.role != UserAuthorityRole.HARBOR_MASTER) { Error.UserNotHarborMaster }

        !LocationRepo.fetchLocationByID(locationId).orDie().onNullFail { Error.LocationNotFound }

        !UserRepo.assignLocationToHarborMaster(userId, locationId, adminId).orDie()

        KIO.unit
    }

    fun changeUserRole(
        userId: Long,
        newRole: UserAuthorityRole
    ): App<Error, Unit> = KIO.comprehension {
        val user = !UserRepo.fetchById(userId).orDie().onNullFail { Error.NotFound }

        !UserRepo.updateUser(
            userId = userId,
            authorityRole = newRole,
            verified = user.verified ?: false
        ).orDie().onNullFail { Error.NotFound }

        KIO.unit
    }
}