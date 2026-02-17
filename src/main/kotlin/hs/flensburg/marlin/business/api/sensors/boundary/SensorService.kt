package hs.flensburg.marlin.business.api.sensors.boundary

import de.lambda9.tailwind.core.KIO
import de.lambda9.tailwind.core.extensions.kio.onNullFail
import de.lambda9.tailwind.core.extensions.kio.orDie
import hs.flensburg.marlin.business.ApiError
import hs.flensburg.marlin.business.App
import hs.flensburg.marlin.business.ServiceLayerError
import hs.flensburg.marlin.business.api.sensors.control.SensorRepo
import hs.flensburg.marlin.business.api.sensors.entity.*
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

    fun getAllMeasurements(): App<Error, List<MeasurementDTO>> = KIO.comprehension {
        val measurements = !SensorRepo.fetchAllMeasurements().orDie().onNullFail { Error.NotFound }
        KIO.ok(measurements.map { it.toMeasurementDTO() })
    }

    fun getLocationsWithLatestMeasurements(timezone: String): App<Error, List<LocationWithLatestMeasurementsDTO>> =
        KIO.comprehension {
            SensorRepo.fetchLocationsWithLatestMeasurements(timezone, "metric").orDie()
                .onNullFail { Error.NotFound }
        }

    fun getLocationsWithLatestMeasurementsNEW(
        timezone: String,
        ipAddress: String,
        units: String
    ): App<Error, List<LocationWithBoxesDTO>> =
        KIO.comprehension {
            val clientTimeZone = TimezonesService.getClientTimeZoneFromIPOrQueryParam(timezone, ipAddress)
            SensorRepo.fetchLocationsWithLatestMeasurements(clientTimeZone, units).orDie()
                .onNullFail { Error.NotFound }
                .map { list ->
                    list.map { it.toLocationWithBoxesDTO() }
                }
        }

    fun getSingleLocationWithLatestMeasurements(
        locationId: Long,
        timezone: String,
        units: String
    ): App<Error, UnitsWithLocationWithBoxesDTO> =
        KIO.comprehension {
            val rawLocation = !SensorRepo.fetchSingleLocationWithLatestMeasurements(
                locationId, timezone, units
            ).orDie().onNullFail { Error.NotFound }
            KIO.ok(mapToUnitsWithLocationWithBoxesDTO(rawLocation))
        }

    fun getLocationByIDWithMeasurementsWithinTimespanFAST(
        locationId: Long,
        timeRange: SensorMeasurementsTimeRange,
        timezone: String,
        ipAddress: String,
        units: String
    ): App<Error, LocationWithBoxesDTO?> = KIO.comprehension {

        SensorRepo.getLatestMeasurementTimeEnriched(
            locationId, timeRange,
            timezone = TimezonesService.getClientTimeZoneFromIPOrQueryParam(timezone, ipAddress),
            units
        ).orDie().onNullFail { Error.NotFound }
            .map { it.toLocationWithBoxesDTO() }
    }

    fun getLocationsWithLatestMeasurementsV3(
        timezone: String,
        ipAddress: String,
        units: String
    ): App<Error, UnitsWithLocationWithBoxesDTO> = KIO.comprehension {
        /*val finalUnits: String = units ?: userId?.let { id ->
            val userView = !UserRepo.fetchViewById(id).orDie()
            userView?.let { view ->
                UserProfile.from(view).measurementSystem?.literal?.lowercase()
            }
        } ?: "metric"*/
        val rawLocations = !SensorRepo.fetchLocationsWithLatestMeasurements(
            !TimezonesService.getClientTimeZoneFromIPOrQueryParam(timezone, ipAddress),
            units
        ).orDie().onNullFail { Error.NotFound }
        KIO.ok(mapToUnitsWithLocationWithBoxesDTO(rawLocations))
    }

    fun getLocationByIDWithMeasurementsWithinTimespanV3(
        locationId: Long,
        timeRange: SensorMeasurementsTimeRange,
        timezone: String,
        ipAddress: String,
        units: String
    ): App<Error, UnitsWithLocationWithBoxesDTO> = KIO.comprehension {
        SensorRepo.getLatestMeasurementTimeEnriched(
            locationId, timeRange,
            timezone = TimezonesService.getClientTimeZoneFromIPOrQueryParam(timezone, ipAddress),
            units
        ).orDie().onNullFail { Error.NotFound }.map { mapToUnitsWithLocationWithBoxesDTO(listOf(it)) }
    }

}