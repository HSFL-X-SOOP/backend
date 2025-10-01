package hs.flensburg.marlin.business.api.sensors.entity.raw

import kotlinx.serialization.Serializable
import hs.flensburg.marlin.database.generated.tables.pojos.Location

@Serializable
data class LocationDTO(
    val id: Long,
    val name: String?,
    val coordinates: GeoPointDTO?
)

@Serializable
data class GeoPointDTO(
    val lat: Double,
    val lon: Double
)

// Annahme: Location.coordinates ist ein org.postgis.Point oder null
fun Location.toLocationDTO() = LocationDTO(
    id = this.id ?: 0L,
    name = this.name,
    coordinates = (this.coordinates as? Pair<Double, Double>)?.let {GeoPointDTO(it.first, it.second)}
)