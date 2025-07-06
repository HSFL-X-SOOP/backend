package hs.flensburg.soop.business.api.dto

import kotlinx.serialization.Serializable


@Serializable
data class LocationWithLatestMeasurementsDTO(
    val location: LocationDTO,
    val latestMeasurements: List<MeasurementDTO>
)