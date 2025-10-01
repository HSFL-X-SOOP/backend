package hs.flensburg.marlin.business.api.sensors.entity.boxes.measurements

import kotlinx.serialization.Serializable

@Serializable
data class WaterTemperatureOnlyMeasurementValuesDTO(
    val waterTemperature: Double?
) : BoxMeasurementsDTO