package hs.flensburg.marlin.business.api.location.entity

import hs.flensburg.marlin.business.api.location.boundary.StringValidationService
import kotlinx.serialization.Serializable

@Serializable
data class UpdateLocationRequest(
    val name: String,
    val description: String?,
    val address: String,
    val openingHours: String?,
    val contact: Contact?,
    val image: ImageRequest?
) {
    init {
        require(name.isNotBlank()) { "Name must not be blank" }
        require(address.isNotBlank()) { "Address must not be blank" }

        val openingHoursError = StringValidationService.validateOpeningHoursFormat(openingHours)
        val contactError = StringValidationService.validateContact(contact)

        require(openingHoursError == null) {
            openingHoursError
                ?: "Unknown error occurred during opening hours validation."
        }

        require(contactError == null) {
            contactError
                ?: "Unknown error occurred during contact validation."
        }
    }
}