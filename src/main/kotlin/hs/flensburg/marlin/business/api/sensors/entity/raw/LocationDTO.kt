package hs.flensburg.marlin.business.api.sensors.entity.raw

import hs.flensburg.marlin.business.api.location.boundary.GeoPoint
import kotlinx.serialization.Serializable
import hs.flensburg.marlin.database.generated.tables.pojos.Location

@Serializable
data class LocationDTO(
    val id: Long,
    val name: String?,
    val coordinates: GeoPoint?
)

fun Location.toLocationDTO() = LocationDTO(
    id = this.id ?: 0L,
    name = this.name,
    coordinates = this.coordinates
)