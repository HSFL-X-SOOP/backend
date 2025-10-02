package hs.flensburg.marlin.business.api.sensors.entity.boxes

import hs.flensburg.marlin.business.api.sensors.entity.boxes.measurements.TimestampedBoxMeasurementsDTO
import hs.flensburg.marlin.business.api.sensors.entity.boxes.measurements.WaterTemperatureOnlyMeasurementValuesDTO
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("WaterTemperatureOnlyBox")
data class WaterTemperatureOnlyBoxDTO(
    val id: Long?,
    val name: String?,
    val description: String?,
    val isMoving: Boolean?,
    val measurementTimes: List<TimestampedBoxMeasurementsDTO<WaterTemperatureOnlyMeasurementValuesDTO>>
) : BoxDTO