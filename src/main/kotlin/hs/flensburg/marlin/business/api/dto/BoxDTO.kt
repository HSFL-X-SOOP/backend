package hs.flensburg.marlin.business.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface BoxDTO

@Serializable
@SerialName("WaterTemperatureOnlyBox")
data class WaterTemperatureOnlyBoxDTO(
    val id: Long?,
    val name: String?,
    val description: String?,
    val isMoving: Boolean?,
    val timestamp: kotlinx.datetime.LocalDateTime?,
    val waterTemperature: Double?
) : BoxDTO

@Serializable
@SerialName("WaterBox")
data class WaterBoxDTO(
    val id: Long?,
    val name: String?,
    val description: String?,
    val isMoving: Boolean?,
    val timestamp: kotlinx.datetime.LocalDateTime?,
    val waterTemperature: Double?,
    val waveHeight: Double?,
    val tide: Double?,
    val standardDeviation: Double?,
    val batteryVoltage: Double?
) : BoxDTO

@Serializable
@SerialName("AirBox")
data class AirBoxDTO(
    val id: Long?,
    val name: String?,
    val description: String?,
    val isMoving: Boolean?,
    val timestamp: kotlinx.datetime.LocalDateTime?,
    val airTemperature: Double?,
    val windSpeed: Double?,
    val windDirection: Double?,
    val gustSpeed: Double?,
    val gustDirection: Double?,
    val humidity: Double?,
    val airPressure: Double?
) : BoxDTO

fun mapSensorToBoxDTO(
    sensor: SensorDTO,
    measurements: List<EnrichedMeasurementDTO>
): BoxDTO {
    // String reliable conversion, be careful with typos
    val waterTemp = measurements.find { it.measurementType.name == "Temperature, water" }?.value
    val waveHeight = measurements.find { it.measurementType.name == "Wave Height" }?.value
    val tide = measurements.find { it.measurementType.name == "Tide" }?.value
    val stdDev = measurements.find { it.measurementType.name == "Standard deviation" }?.value
    val battery = measurements.find { it.measurementType.name == "Battery, voltage" }?.value

    val airTemp = measurements.find { it.measurementType.name == "Temperature, air" }?.value
    val windSpeed = measurements.find { it.measurementType.name == "Wind speed" }?.value
    val windDir = measurements.find { it.measurementType.name == "Wind direction" }?.value
    val gustSpeed = measurements.find { it.measurementType.name == "Wind speed, gust" }?.value
    val gustDir = measurements.find { it.measurementType.name == "Wind direction, gust" }?.value
    val humidity = measurements.find { it.measurementType.name == "Humidity, relative" }?.value
    val pressure = measurements.find { it.measurementType.name == "Station pressure" }?.value

    return when {
        // Only water temperature
        waveHeight == null && tide == null && stdDev == null && battery == null && waterTemp != null -> WaterTemperatureOnlyBoxDTO(
            id = sensor.id,
            name = sensor.name,
            description = sensor.description,
            isMoving = sensor.isMoving,
            timestamp = measurements.firstOrNull()?.time,
            waterTemperature = waterTemp
        )
        // Water box with multiple measurements
        waveHeight != null || tide != null || stdDev != null -> WaterBoxDTO(
            id = sensor.id,
            name = sensor.name,
            description = sensor.description,
            isMoving = sensor.isMoving,
            timestamp = measurements.firstOrNull()?.time,
            waterTemperature = waterTemp,
            waveHeight = waveHeight,
            tide = tide,
            standardDeviation = stdDev,
            batteryVoltage = battery
        )
        // Air box
        airTemp != null || windSpeed != null || windDir != null -> AirBoxDTO(
            id = sensor.id,
            name = sensor.name,
            description = sensor.description,
            isMoving = sensor.isMoving,
            timestamp = measurements.firstOrNull()?.time,
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
