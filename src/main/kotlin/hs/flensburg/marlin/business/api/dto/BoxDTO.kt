package hs.flensburg.marlin.business.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.datetime.LocalDateTime

@Serializable
sealed interface BoxDTO

@Serializable
@SerialName("WaterTemperatureOnlyBox")
data class WaterTemperatureOnlyBoxDTO(
    val id: Long?,
    val name: String?,
    val description: String?,
    val isMoving: Boolean?,
    val measurementTimes: Map<LocalDateTime, WaterTemperatureOnlyMeasurementValuesDTO>
) : BoxDTO


@Serializable
@SerialName("WaterBox")
data class WaterBoxDTO(
    val id: Long?,
    val name: String?,
    val description: String?,
    val isMoving: Boolean?,
    val measurementTimes: Map<LocalDateTime, WaterMeasurementValuesDTO>
) : BoxDTO


@Serializable
@SerialName("AirBox")
data class AirBoxDTO(
    val id: Long?,
    val name: String?,
    val description: String?,
    val isMoving: Boolean?,
    val measurementTimes: Map<LocalDateTime, AirMeasurementValuesDTO>
) : BoxDTO

fun mapSensorToBoxDTO(
    sensor: SensorDTO,
    measurements: List<EnrichedMeasurementDTO>
): BoxDTO {
    // group measurements by timestamp
    val measurementsByTime = measurements.groupBy { it.time!! }

    // build values per timestamp
    val measurementTimes: Map<LocalDateTime, BoxMeasurementsDTO> = measurementsByTime.mapValues { (_, ms) ->
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

        when {
            // water only
            waveHeight == null && tide == null && stdDev == null && battery == null && waterTemp != null ->
                WaterTemperatureOnlyMeasurementValuesDTO(
                    waterTemperature = waterTemp
                )

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
    }

    // decide which box DTO to return
    return when (measurementTimes.values.first()) {
        is WaterTemperatureOnlyMeasurementValuesDTO -> WaterTemperatureOnlyBoxDTO(
            id = sensor.id,
            name = sensor.name,
            description = sensor.description,
            isMoving = sensor.isMoving,
            measurementTimes = measurementTimes as Map<LocalDateTime, WaterTemperatureOnlyMeasurementValuesDTO>
        )

        is WaterMeasurementValuesDTO -> WaterBoxDTO(
            id = sensor.id,
            name = sensor.name,
            description = sensor.description,
            isMoving = sensor.isMoving,
            measurementTimes = measurementTimes as Map<LocalDateTime, WaterMeasurementValuesDTO>
        )

        is AirMeasurementValuesDTO -> AirBoxDTO(
            id = sensor.id,
            name = sensor.name,
            description = sensor.description,
            isMoving = sensor.isMoving,
            measurementTimes = measurementTimes as Map<LocalDateTime, AirMeasurementValuesDTO>
        )

        else -> throw IllegalArgumentException("Unsupported measurement type for ${sensor.name}")
    }
}
