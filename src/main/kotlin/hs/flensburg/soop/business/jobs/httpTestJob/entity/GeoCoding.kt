package hs.flensburg.soop.business.jobs.reverseGeocoding.entity

import kotlinx.serialization.Serializable

@Serializable
data class NominatimResponse(
    val display_name: String? = null,
    val error: String? = null
)
