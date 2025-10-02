package hs.flensburg.marlin.business.api.sensors.entity

import hs.flensburg.marlin.business.api.sensors.entity.raw.MeasurementTypeDTO
import hs.flensburg.marlin.business.api.sensors.entity.raw.SensorDTO
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class EnrichedMeasurementDTO(
    val sensor: SensorDTO,
    val measurementType: MeasurementTypeDTO,
    val time: LocalDateTime?,
    val value: Double
)
