package hs.flensburg.marlin.business.api.location.entity

import hs.flensburg.marlin.business.api.sensors.entity.raw.GeoPointDTO
import hs.flensburg.marlin.business.api.sensors.entity.raw.LocationDTO
import hs.flensburg.marlin.database.generated.tables.pojos.Location
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.toKotlinLocalTime
import kotlinx.serialization.Serializable

@Serializable
data class DetailedLocationDTO(
    val id: Long,
    val name: String?,
    val description: String?,
    val address: String?,
    val openingTime: LocalTime?,
    val closingTime: LocalTime?,
    val coordinates: GeoPointDTO?
)

fun Location.toDetailedLocationDTO() = DetailedLocationDTO(
    id = this.id ?: 0L,
    name = this.name,
    description = this.description,
    address = this.address,
    openingTime = this.openingTime?.toKotlinLocalTime(),
    closingTime = this.closingTime?.toKotlinLocalTime(),
    coordinates = (this.coordinates as? Pair<Double, Double>)?.let {GeoPointDTO(it.first, it.second)}
)
