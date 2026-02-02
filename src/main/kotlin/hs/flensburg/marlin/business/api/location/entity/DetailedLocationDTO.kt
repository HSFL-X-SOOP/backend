package hs.flensburg.marlin.business.api.location.entity

import hs.flensburg.marlin.business.api.timezones.boundary.TimezonesService
import hs.flensburg.marlin.database.generated.tables.pojos.Location
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import java.time.ZoneOffset

@Serializable
data class DetailedLocationDTO(
    val id: Long?,
    val name: String?,
    val description: String?,
    val address: String?,
    val openingHours: String?,
    val contact: Contact?,
    val coordinates: GeoPoint?,
    val operationalSince: LocalDate?
) {
    companion object {
        fun fromLocation(location: Location, timezone: String?): DetailedLocationDTO {
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
                coordinates = location.coordinates,
                operationalSince = TimezonesService.toLocalDateInZone(
                    location.createdAt!!.atOffset(ZoneOffset.UTC),
                    timezone
                )
            )
        }
    }
}

@Serializable
data class Contact(val phone: String?, val email: String?, val website: String?)

