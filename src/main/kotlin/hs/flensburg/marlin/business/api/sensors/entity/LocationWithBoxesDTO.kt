package hs.flensburg.marlin.business.api.sensors.entity

import hs.flensburg.marlin.business.api.sensors.entity.boxes.BoxDTO
import hs.flensburg.marlin.business.api.sensors.entity.raw.LocationDTO
import kotlinx.serialization.Serializable

@Serializable
data class LocationWithBoxesDTO(
    val location: LocationDTO,
    val boxes: List<BoxDTO>
)