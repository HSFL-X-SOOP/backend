package hs.flensburg.marlin.business.api.auth.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


// Further documentation under https://ipapi.co/api/#complete-location
@Serializable
data class IPAddressLookupResponse(
    val ip: String? = null,
    val hostname: String? = null,
    val city: String? = null,
    val region: String? = null,
    val country: String? = null,
    val loc: String? = null,
    val org: String? = null,
    val postal: String? = null,
    val timezone: String? = null,
    val anycast: Boolean? = null
)