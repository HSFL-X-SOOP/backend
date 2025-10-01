package hs.flensburg.marlin.business.api.sensors.entity.boxes

import hs.flensburg.marlin.business.api.sensors.entity.boxes.measurements.TimestampedBoxMeasurementsDTO
import hs.flensburg.marlin.business.api.sensors.entity.boxes.measurements.WaterMeasurementValuesDTO
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("WaterBox")
data class WaterBoxDTO(
    val id: Long?,
    val name: String?,
    val description: String?,
    val isMoving: Boolean?,
    val measurementTimes: List<TimestampedBoxMeasurementsDTO<WaterMeasurementValuesDTO>>
) : BoxDTO