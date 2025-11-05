package hs.flensburg.marlin.business.api.location.entity

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
) {
    companion object {
        fun fromLocation(location: Location): DetailedLocationDTO {
            return DetailedLocationDTO(
                id = location.id,
                name = location.name,
                description = location.description,
                address = location.address,
                openingTime = location.openingTime?.toKotlinLocalTime(),
                closingTime = location.closingTime?.toKotlinLocalTime(),
                coordinates = location.coordinates
            )
        }
    }
}

