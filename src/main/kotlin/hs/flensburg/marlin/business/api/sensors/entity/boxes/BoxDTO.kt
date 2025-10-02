package hs.flensburg.marlin.business.api.sensors.entity.boxes

import hs.flensburg.marlin.business.api.sensors.entity.boxes.measurements.AirMeasurementValuesDTO
import hs.flensburg.marlin.business.api.sensors.entity.boxes.measurements.BoxMeasurementsDTO
import hs.flensburg.marlin.business.api.sensors.entity.boxes.measurements.TimestampedBoxMeasurementsDTO
import hs.flensburg.marlin.business.api.sensors.entity.boxes.measurements.WaterMeasurementValuesDTO
import hs.flensburg.marlin.business.api.sensors.entity.boxes.measurements.WaterTemperatureOnlyMeasurementValuesDTO
import hs.flensburg.marlin.business.api.sensors.entity.EnrichedMeasurementDTO
import hs.flensburg.marlin.business.api.sensors.entity.raw.SensorDTO
import kotlinx.serialization.Serializable

@Serializable
sealed interface BoxDTO


fun mapSensorToBoxDTO(
    sensor: SensorDTO,
    measurements: List<EnrichedMeasurementDTO>
): BoxDTO {
    // group measurements by timestamp
    val measurementsByTime = measurements.groupBy { it.time!! }

// build values per timestamp
    val measurementTimes: List<TimestampedBoxMeasurementsDTO<out BoxMeasurementsDTO>> =
        measurementsByTime.map { (time, ms) ->
            val waterTemp = ms.find { it.measurementType.name == "Temperature, water" }?.value
            val waveHeight = ms.find { it.measurementType.name == "Wave Height" }?.value
            val tide = ms.find { it.measurementType.name == "Tide" }?.value
            val stdDev = ms.find { it.measurementType.name == "Standard deviation" }?.value
            val battery = ms.find { it.measurementType.name == "Battery, voltage" }?.value

            val airTemp = ms.find { it.measurementType.name == "Temperature, air" }?.value
            val windSpeed = ms.find { it.measurementType.name == "Wind speed" }?.value
            val windDir = ms.find { it.measurementType.name == "Wind direction" }?.value
            val gustSpeed = ms.find { it.measurementType.name == "Wind speed, gust" }?.value
            val gustDir = ms.find { it.measurementType.name == "Wind direction, gust" }?.value
            val humidity = ms.find { it.measurementType.name == "Humidity, relative" }?.value
            val pressure = ms.find { it.measurementType.name == "Station pressure" }?.value

            val values: BoxMeasurementsDTO = when {
                // water only
                waveHeight == null && tide == null && stdDev == null && battery == null && waterTemp != null ->
                    WaterTemperatureOnlyMeasurementValuesDTO(waterTemperature = waterTemp)

                // water box
                waterTemp != null || waveHeight != null || tide != null || stdDev != null || battery != null ->
                    WaterMeasurementValuesDTO(
                        waterTemperature = waterTemp,
                        waveHeight = waveHeight,
                        tide = tide,
                        standardDeviation = stdDev,
                        batteryVoltage = battery
                    )

                // air box
                airTemp != null || windSpeed != null || windDir != null || gustSpeed != null ||
                        gustDir != null || humidity != null || pressure != null ->
                    AirMeasurementValuesDTO(
                        airTemperature = airTemp,
                        windSpeed = windSpeed,
                        windDirection = windDir,
                        gustSpeed = gustSpeed,
                        gustDirection = gustDir,
                        humidity = humidity,
                        airPressure = pressure
                    )

                else -> throw IllegalArgumentException("Unknown sensor type for ${sensor.name}")
            }

            TimestampedBoxMeasurementsDTO(
                time = time,
                measurements = values
            )
        }

// decide which box DTO to return
    return when (val first = measurementTimes.first().measurements) {
        is WaterTemperatureOnlyMeasurementValuesDTO -> WaterTemperatureOnlyBoxDTO(
            id = sensor.id,
            name = sensor.name,
            description = sensor.description,
            isMoving = sensor.isMoving,
            measurementTimes = measurementTimes.map {
                TimestampedBoxMeasurementsDTO(it.time, it.measurements as WaterTemperatureOnlyMeasurementValuesDTO)
            }
        )

        is WaterMeasurementValuesDTO -> WaterBoxDTO(
            id = sensor.id,
            name = sensor.name,
            description = sensor.description,
            isMoving = sensor.isMoving,
            measurementTimes = measurementTimes.map {
                TimestampedBoxMeasurementsDTO(it.time, it.measurements as WaterMeasurementValuesDTO)
            }
        )

        is AirMeasurementValuesDTO -> AirBoxDTO(
            id = sensor.id,
            name = sensor.name,
            description = sensor.description,
            isMoving = sensor.isMoving,
            measurementTimes = measurementTimes.map {
                TimestampedBoxMeasurementsDTO(it.time, it.measurements as AirMeasurementValuesDTO)
            }
        )
    }
}
