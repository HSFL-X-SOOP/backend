package hs.flensburg.soop.business.api.dto

import kotlinx.serialization.Serializable
import kotlinx.datetime.LocalDateTime


@Serializable
data class LocationWithLatestMeasurementsDTO(
    val location: LocationDTO,
    val latestMeasurements: List<EnrichedMeasurementDTO>
)

@Serializable
data class EnrichedMeasurementDTO(
    val sensor: SensorDTO,
    val measurementType: MeasurementTypeDTO,
    val time: LocalDateTime?,
    val value: Double
)

