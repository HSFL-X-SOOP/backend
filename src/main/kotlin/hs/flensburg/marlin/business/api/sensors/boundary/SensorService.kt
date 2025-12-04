package hs.flensburg.marlin.business.api.sensors.boundary

import de.lambda9.tailwind.core.KIO
import de.lambda9.tailwind.core.extensions.kio.onNullFail
import de.lambda9.tailwind.core.extensions.kio.orDie
import hs.flensburg.marlin.business.ApiError
import hs.flensburg.marlin.business.App
import hs.flensburg.marlin.business.ServiceLayerError
import hs.flensburg.marlin.business.api.sensors.control.SensorRepo
import hs.flensburg.marlin.business.api.sensors.entity.LocationWithBoxesDTO
import hs.flensburg.marlin.business.api.sensors.entity.LocationWithLatestMeasurementsDTO
import hs.flensburg.marlin.business.api.sensors.entity.mapToLocationWithBoxesDTO
import hs.flensburg.marlin.business.api.sensors.entity.raw.*
import hs.flensburg.marlin.business.api.timezones.boundary.TimezonesService

object SensorService {
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

    fun getAllSensors(): App<Error, List<SensorDTO>> = KIO.comprehension {
        val sensors = !SensorRepo.fetchAllSensors().orDie().onNullFail { Error.NotFound }
        KIO.ok(sensors.map { it.toSensorDTO() })
    }

    fun getAllMeasurementTypes(): App<Error, List<MeasurementTypeDTO>> = KIO.comprehension {
        val measurementTypes = !SensorRepo.fetchAllMeasurementTypes().orDie().onNullFail { Error.NotFound }
        KIO.ok(measurementTypes.map { it.toMeasurementTypeDTO() })
    }

    fun getAllLocations(): App<Error, List<LocationDTO>> = KIO.comprehension {
        val locations = !SensorRepo.fetchAllLocations().orDie().onNullFail { Error.NotFound }
        KIO.ok(locations.map { LocationDTO.fromLocation(it) })
    }

    fun getAllMeasurements(): App<Error, List<MeasurementDTO>> = KIO.comprehension {
        val measurements = !SensorRepo.fetchAllMeasurements().orDie().onNullFail { Error.NotFound }
        KIO.ok(measurements.map { it.toMeasurementDTO() })
    }

    fun getLocationsWithLatestMeasurements(timezone: String): App<Error, List<LocationWithLatestMeasurementsDTO>> =
        KIO.comprehension {
            val latestMeasurements =
                !SensorRepo.fetchLocationsWithLatestMeasurements(timezone).orDie().onNullFail { Error.NotFound }
            KIO.ok(latestMeasurements)
        }

    fun getLocationWithLatestMeasurementsNEW(
        timezone: String,
        ipAddress: String
    ): App<Error, List<LocationWithBoxesDTO>> =
        KIO.comprehension {
            val clientTimeZone = !TimezonesService.getClientTimeZoneFromIPOrQueryParam(timezone, ipAddress)
            val rawLocations =
                !SensorRepo.fetchLocationsWithLatestMeasurements(clientTimeZone).orDie().onNullFail { Error.NotFound }
            KIO.ok(rawLocations.map { it.mapToLocationWithBoxesDTO() })
        }


    fun getLocationByIDWithMeasurementsWithinTimespan(
        locationId: Long,
        timeRange: String, // "today", "week", "month"
        timezone: String,
        ipAddress: String
    ): App<Error, LocationWithBoxesDTO?> = KIO.comprehension {
        val rawLocation = !SensorRepo.fetchLocationByIDWithMeasurementsWithinTimespan(
            locationId,
            timeRange,
            !TimezonesService.getClientTimeZoneFromIPOrQueryParam(timezone, ipAddress)
        ).orDie().onNullFail { Error.NotFound }
        KIO.ok(rawLocation.mapToLocationWithBoxesDTO())
    }

    fun getLocationByIDWithMeasurementsWithinTimespanFASTER(
        locationId: Long,
        timeRange: String, // "today", "week", "month"
        timezone: String,
        ipAddress: String
    ): App<Error, LocationWithBoxesDTO?> = KIO.comprehension {
        val validTimeRange = when (timeRange.lowercase()) {
            "24h" -> true
            "48h" -> true
            "7d" -> true
            "30d" -> true
            "90d" -> true
            "180d" -> true
            "1y" -> true
            else -> false
        }
        !KIO.failOn(!validTimeRange) { Error.BadRequest }

        val data = !SensorRepo.getLatestMeasurementTimeEnriched(
            locationId, timeRange,
            timezone = !TimezonesService.getClientTimeZoneFromIPOrQueryParam(timezone, ipAddress)
        ).orDie().onNullFail { Error.NotFound }
        KIO.ok(data.mapToLocationWithBoxesDTO())
    }
}