package hs.flensburg.marlin.business.api.sensors.entity.boxes.measurements

import kotlinx.serialization.Serializable

@Serializable
data class WaterMeasurementValuesDTO(
    val waterTemperature: Double?,
    val waveHeight: Double?,
    val tide: Double?,
    val standardDeviation: Double?,
    val batteryVoltage: Double?
) : BoxMeasurementsDTO