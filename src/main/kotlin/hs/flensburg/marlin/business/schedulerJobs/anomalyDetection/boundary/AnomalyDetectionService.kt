package hs.flensburg.marlin.business.schedulerJobs.anomalyDetection.boundary

import de.lambda9.tailwind.core.KIO
import de.lambda9.tailwind.core.extensions.kio.onNullFail
import de.lambda9.tailwind.core.extensions.kio.orDie
import hs.flensburg.marlin.business.ApiError
import hs.flensburg.marlin.business.App
import hs.flensburg.marlin.business.ServiceLayerError
import hs.flensburg.marlin.business.api.sensors.control.SensorRepo
import hs.flensburg.marlin.business.api.sensors.entity.EnrichedMeasurementDTO
import hs.flensburg.marlin.business.api.sensors.entity.LocationWithLatestMeasurementsDTO
import hs.flensburg.marlin.business.api.sensors.entity.raw.MeasurementTypeDTO
import hs.flensburg.marlin.business.schedulerJobs.anomalyDetection.control.AnomalyDetectionRepo.writeAnomaly
import java.security.MessageDigest
import kotlin.math.abs

object AnomalyDetectionService {
    //Define constants for all important boundary values of the detection
    const val MIN_WATER_TEMPERATURE: Double = -2.0
    const val MAX_WATER_TEMPERATURE: Double = 40.0
    const val TEMP_SENSOR_DEPTH_LOWER_BORDER: Double = -32.0
    const val TEMP_SENSOR_DEPTH_UPPER_BORDER: Double = 40.0
    const val WATER_LEVEL_SENSOR_HEIGHT: Double = 200.0
    const val WATER_LEVEL_SENSOR_BLIND_ZONE: Double = 20.0
    const val WATER_LEVEL_SENSOR_MAX_MEASURING_DISTANCE: Double = 700.0
    const val WATER_LEVEL_SENSOR_UPPER_LIMIT: Double = WATER_LEVEL_SENSOR_HEIGHT - WATER_LEVEL_SENSOR_BLIND_ZONE
    const val WATER_LEVEL_SENSOR_LOWER_LIMIT: Double =
        WATER_LEVEL_SENSOR_HEIGHT - WATER_LEVEL_SENSOR_MAX_MEASURING_DISTANCE
    const val MAX_WATER_LEVEL_FROM_MEDIAN: Double = 20.0
    const val MAX_TEMPERATURE_FROM_MEDIAN: Double = 1.5

    // Fixed Values from the DB
    const val MEASUREMENT_NAME_TEMPERATURE = "Temperature, water"
    const val MEASUREMENT_NAME_WAVE_HEIGHT = "Wave Height"
    const val MEASUREMENT_NAME_WATER_LEVEL = "Tide"

    sealed class Error(private val message: String) : ServiceLayerError {
        object NotFound : Error("Location, Sensor or measurement not found")

        override fun toApiError(): ApiError {
            return when (this) {
                is NotFound -> ApiError.NotFound(message)
            }
        }
    }

    val history = mutableListOf<String>()

    fun checkNewMeasurements(locationId: Long): App<ServiceLayerError, Unit> = KIO.comprehension {
        print("Check gestartet")
        val latestMeasurements =
            !SensorRepo.fetchSingleLocationWithLatestMeasurements(locationId, "UTC", "metric").orDie()
                .onNullFail { Error.NotFound }
        val currentMeasurement = latestMeasurements.first()

        val hash = getHash(currentMeasurement)

        if (hash !in history) {
            val pastMeasurements =
                !SensorRepo.fetchMeasurementsWithinCustomTimeRange(locationId, "3h", "UTC", "metric").orDie()
                    .onNullFail { Error.NotFound }

            detectAnomaly(
                currentMeasurement.latestMeasurements,
                pastMeasurements.latestMeasurements - currentMeasurement.latestMeasurements.toSet()
            )
            history.addLast(hash)
        }
        if (history.size > 300) history.removeFirst()
        KIO.unit
    }

    private fun detectAnomaly(
        measurement: List<EnrichedMeasurementDTO>, pastMeasurements: List<EnrichedMeasurementDTO>
    ) {
        print("Check gestartet für ein Measurement")
        var anomalousWaterLevel = false
        var anomalousWaveHeight = false

        val current = measurement.associateBy { it.measurementType.name }
        val currentWaterTemp = current[MEASUREMENT_NAME_TEMPERATURE]
        val currentWaveHeight = current[MEASUREMENT_NAME_WAVE_HEIGHT]
        val currentWaterLevel = current[MEASUREMENT_NAME_WATER_LEVEL]

        val pastMeasurementsByType = pastMeasurements.groupBy { it.measurementType }

        var anomalousWaterTemp = currentWaterTemp?.let {
            checkTempAnomalies(it, pastMeasurementsByType)
        } ?: false

        if (currentWaterLevel != null && currentWaveHeight != null) {
            anomalousWaterLevel =
                currentWaterLevel.value !in WATER_LEVEL_SENSOR_LOWER_LIMIT..WATER_LEVEL_SENSOR_UPPER_LIMIT
            anomalousWaveHeight =
                currentWaterLevel.value + currentWaveHeight.value !in 0.0..WATER_LEVEL_SENSOR_UPPER_LIMIT
            if (!anomalousWaterTemp) anomalousWaterTemp =
                currentWaterLevel.value !in TEMP_SENSOR_DEPTH_LOWER_BORDER..TEMP_SENSOR_DEPTH_UPPER_BORDER

            if (!anomalousWaterLevel) {
                val absDiffFromMedianWaterLevel =
                    abs(currentWaterLevel.value - calculateMedian(pastMeasurementsByType, MEASUREMENT_NAME_WATER_LEVEL))
                anomalousWaterLevel = absDiffFromMedianWaterLevel >= MAX_WATER_LEVEL_FROM_MEDIAN
            }
        }

        // TODO("ggf. Prüfung des Zusammenspiels mit anderen Sensoren")

        currentWaterTemp.takeIf { anomalousWaterTemp }?.let(::writeAnomaly)
        currentWaterLevel.takeIf { anomalousWaterLevel }?.let(::writeAnomaly)
        currentWaveHeight.takeIf { anomalousWaveHeight }?.let(::writeAnomaly)
    }

    private fun checkTempAnomalies(
        temperature: EnrichedMeasurementDTO,
        pastMeasurementsByType: Map<MeasurementTypeDTO, List<EnrichedMeasurementDTO>>
    ): Boolean {
        if (temperature.value !in MIN_WATER_TEMPERATURE..MAX_WATER_TEMPERATURE) {
            return true
        }
        val absDiffFromMedianWaterTemperature =
            abs(temperature.value - calculateMedian(pastMeasurementsByType, MEASUREMENT_NAME_TEMPERATURE))
        if (absDiffFromMedianWaterTemperature >= MAX_TEMPERATURE_FROM_MEDIAN) {
            return true
        }
        return false
    }

    private fun getHash(measurement: LocationWithLatestMeasurementsDTO): String {
        val bytes = measurement.toString().toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)

        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun calculateMedian(
        map: Map<MeasurementTypeDTO, List<EnrichedMeasurementDTO>>, measurementTypeName: String
    ): Double {
        print("Median wird berechnet")
        val key = map.keys.firstOrNull { it.name == measurementTypeName }!!
        val list = map[key]

        val sortedList = list!!.sortedBy { it.value }

        val size = sortedList.size
        return if (size % 2 == 1) {
            sortedList[size / 2].value
        } else {
            val mid1 = sortedList[(size / 2) - 1].value
            val mid2 = sortedList[size / 2].value
            (mid1 + mid2) / 2.0
        }
    }
}