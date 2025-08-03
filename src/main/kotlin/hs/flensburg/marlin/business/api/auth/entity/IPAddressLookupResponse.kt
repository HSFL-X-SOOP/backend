package hs.flensburg.marlin.business.api.auth.entity

import kotlinx.serialization.Serializable

@Serializable
data class IPAddressLookupResponse(
    val status: String,
    val message: String? = null,
    val country: String? = null,
    val regionName: String? = null,
    val city: String? = null,
    val lat: Double? = null,
    val lon: Double? = null,
    val timezone: String? = null,
    val query: String? = null
)