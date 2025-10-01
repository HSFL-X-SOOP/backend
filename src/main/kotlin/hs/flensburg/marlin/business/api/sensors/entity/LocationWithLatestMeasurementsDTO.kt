package hs.flensburg.marlin.business.api.sensors.entity

import hs.flensburg.marlin.business.api.sensors.entity.raw.LocationDTO
import kotlinx.serialization.Serializable

@Serializable
data class LocationWithLatestMeasurementsDTO(
    val location: LocationDTO,
    val latestMeasurements: List<EnrichedMeasurementDTO>
)