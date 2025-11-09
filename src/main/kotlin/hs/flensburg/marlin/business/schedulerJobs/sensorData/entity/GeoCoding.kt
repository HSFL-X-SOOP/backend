package hs.flensburg.marlin.business.schedulerJobs.sensorData.entity

import kotlinx.serialization.Serializable

@Serializable
data class NominatimResponse(
    val display_name: String? = null,
    val error: String? = null,
    val address: Address? = null
)

@Serializable
data class Address(
    val city: String? = null,
    val town: String? = null,
    val village: String? = null,
    val suburb: String? = null
)
