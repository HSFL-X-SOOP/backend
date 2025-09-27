package hs.flensburg.marlin.business.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class LocationWithBoxesDTO(
    val location: LocationDTO,
    val boxes: List<BoxDTO>
)

