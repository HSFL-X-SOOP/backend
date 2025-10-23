package hs.flensburg.marlin.business.api.location.boundary

import kotlinx.serialization.Serializable

@Serializable
data class GeoPoint(
    val lat: Double,
    val lon: Double
)
