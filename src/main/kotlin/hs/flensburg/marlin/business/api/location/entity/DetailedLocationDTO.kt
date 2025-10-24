package hs.flensburg.marlin.business.api.location.entity

import hs.flensburg.marlin.business.api.location.boundary.GeoPoint
import hs.flensburg.marlin.database.generated.tables.pojos.Location
import kotlinx.datetime.LocalTime
import kotlinx.datetime.toKotlinLocalTime
import kotlinx.serialization.Serializable

@Serializable
data class DetailedLocationDTO(
    val id: Long?,
    val name: String?,
    val description: String?,
    val address: String?,
    val openingTime: LocalTime?,
    val closingTime: LocalTime?,
    val coordinates: GeoPoint?
)

fun Location.toDetailedLocationDTO() = DetailedLocationDTO(
    id = this.id,
    name = this.name,
    description = this.description,
    address = this.address,
    openingTime = this.openingTime?.toKotlinLocalTime(),
    closingTime = this.closingTime?.toKotlinLocalTime(),
    coordinates = this.coordinates
)
