package hs.flensburg.marlin.business.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.datetime.LocalDateTime

@Serializable
sealed interface BoxMeasurementsDTO

@Serializable
data class WaterTemperatureOnlyMeasurementValuesDTO(
    val waterTemperature: Double?
) : BoxMeasurementsDTO

@Serializable
data class WaterMeasurementValuesDTO(
    val waterTemperature: Double?,
    val waveHeight: Double?,
    val tide: Double?,
    val standardDeviation: Double?,
    val batteryVoltage: Double?
) : BoxMeasurementsDTO

@Serializable
data class AirMeasurementValuesDTO(
    val airTemperature: Double?,
    val windSpeed: Double?,
    val windDirection: Double?,
    val gustSpeed: Double?,
    val gustDirection: Double?,
    val humidity: Double?,
    val airPressure: Double?
) : BoxMeasurementsDTO

@Serializable
data class TimestampedBoxMeasurementsDTO<T : BoxMeasurementsDTO>(
    val time: LocalDateTime,
    val measurements: T
)
