package hs.flensburg.marlin.business.api.location.entity

import hs.flensburg.marlin.database.generated.tables.pojos.Location
import kotlinx.serialization.Serializable

@Serializable
data class DetailedLocationDTO(
    val id: Long?,
    val name: String?,
    val description: String?,
    val address: String?,
    val openingHours: String?,
    val contact: Contact?,
    val coordinates: GeoPoint?
) {
    companion object {
        fun fromLocation(location: Location): DetailedLocationDTO {
            return DetailedLocationDTO(
                id = location.id,
                name = location.name,
                description = location.description,
                address = location.address,
                openingHours = location.openingHours,
                contact = Contact(
                        location.contactPhone,
                        location.contactEmail,
                        location.contactWebsite
                    ),
                coordinates = location.coordinates
            )
        }
    }
}

@Serializable
data class Contact(val phone: String?, val email: String?, val website: String?)

