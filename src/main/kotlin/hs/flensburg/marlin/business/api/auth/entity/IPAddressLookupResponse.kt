package hs.flensburg.marlin.business.api.auth.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


// Further documentation under https://ipapi.co/api/#complete-location
@Serializable
data class IPAddressLookupResponse(
    val ip: String? = null,
    val network: String? = null,
    val version: String? = null,
    val city: String? = null,
    val region: String? = null,
    @SerialName("region_code") val regionCode: String? = null,

    val country: String? = null,
    @SerialName("country_name") val countryName: String? = null,
    @SerialName("country_code") val countryCode: String? = null,
    @SerialName("country_code_iso3") val countryCodeIso3: String? = null,
    @SerialName("country_capital") val countryCapital: String? = null,
    @SerialName("country_tld") val countryTld: String? = null,

    @SerialName("continent_code") val continentCode: String? = null,
    @SerialName("in_eu") val inEu: Boolean? = null,
    val postal: String? = null,

    @SerialName("latitude") val latitude: Double? = null,
    @SerialName("longitude") val longitude: Double? = null,

    val timezone: String? = null,
    @SerialName("utc_offset") val utcOffset: String? = null,
    @SerialName("country_calling_code") val countryCallingCode: String? = null,

    val currency: String? = null,
    @SerialName("currency_name") val currencyName: String? = null,
    val languages: String? = null,

    @SerialName("country_area") val countryArea: Double? = null,
    @SerialName("country_population") val countryPopulation: Long? = null,

    val asn: String? = null,
    val org: String? = null
)