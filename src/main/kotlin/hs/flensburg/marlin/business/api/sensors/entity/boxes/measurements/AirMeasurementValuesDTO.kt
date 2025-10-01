package hs.flensburg.marlin.business.api.sensors.entity.boxes.measurements

import kotlinx.serialization.Serializable

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