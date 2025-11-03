package hs.flensburg.marlin.business.api.admin.boundary

import de.lambda9.tailwind.core.KIO
import de.lambda9.tailwind.core.extensions.kio.onNullFail
import de.lambda9.tailwind.core.extensions.kio.orDie
import hs.flensburg.marlin.business.ApiError
import hs.flensburg.marlin.business.ServiceLayerError
import hs.flensburg.marlin.business.api.admin.entity.Dashboard
import hs.flensburg.marlin.business.App
import hs.flensburg.marlin.business.api.location.control.LocationRepo
import hs.flensburg.marlin.business.api.sensors.control.SensorRepo
import hs.flensburg.marlin.business.api.users.control.UserRepo

object AdminService {
    sealed class Error(private val message: String) : ServiceLayerError {
        object NotFound : Error("Location, Sensor or measurement not found")
        object BadRequest : Error("Bad request")

        override fun toApiError(): ApiError {
            return when (this) {
                is NotFound -> ApiError.NotFound(message)
                is BadRequest -> ApiError.BadRequest(message)
            }
        }
    }

    fun getDashboardInformation(): App<Error, Dashboard.DashboardInfo> = KIO.comprehension {

        val totalSensors = !SensorRepo.countAllActiveSensors().orDie().onNullFail { Error.NotFound }
        val totalMeasurement = !SensorRepo.countAllMeasurementsToday().orDie().onNullFail { Error.NotFound }
        val totalLocations = !LocationRepo.countAllLocations().orDie().onNullFail { Error.NotFound }
        val totalUsers = !UserRepo.countAllUsers().orDie().onNullFail { Error.NotFound }

        val dashboardInfo = Dashboard.DashboardInfo(
            totalLocations = totalLocations,
            totalSensors = totalSensors,
            totalMeasurements = totalMeasurement,
            totalUsers = totalUsers
        )

        KIO.ok(dashboardInfo)

    }
}