package hs.flensburg.marlin.business.api.sensors.entity.boxes.measurements

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class TimestampedBoxMeasurementsDTO<T : BoxMeasurementsDTO>(
    val time: LocalDateTime,
    val measurements: T
)