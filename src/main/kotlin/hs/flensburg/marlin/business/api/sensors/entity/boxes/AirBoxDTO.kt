package hs.flensburg.marlin.business.api.sensors.entity.boxes

import hs.flensburg.marlin.business.api.sensors.entity.boxes.measurements.AirMeasurementValuesDTO
import hs.flensburg.marlin.business.api.sensors.entity.boxes.measurements.TimestampedBoxMeasurementsDTO
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("AirBox")
data class AirBoxDTO(
    val id: Long?,
    val name: String?,
    val description: String?,
    val isMoving: Boolean?,
    val measurementTimes: List<TimestampedBoxMeasurementsDTO<AirMeasurementValuesDTO>>
) : BoxDTO