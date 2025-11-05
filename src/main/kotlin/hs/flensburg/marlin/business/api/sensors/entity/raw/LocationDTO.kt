package hs.flensburg.marlin.business.api.sensors.entity.raw

import hs.flensburg.marlin.business.api.location.entity.GeoPoint
import kotlinx.serialization.Serializable
import hs.flensburg.marlin.database.generated.tables.pojos.Location

@Serializable
data class LocationDTO(
    val id: Long,
    val name: String?,
    val coordinates: GeoPoint?
){
    companion object{
        fun fromLocation(location: Location): LocationDTO {
            return LocationDTO(
                id = location.id ?: 0L,
                name = location.name,
                coordinates = location.coordinates
            )
        }
    }
}