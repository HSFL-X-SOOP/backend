package hs.flensburg.marlin.business.api.location.entity

import kotlinx.datetime.LocalTime
import kotlinx.serialization.Serializable

@Serializable
data class UpdateLocationRequest(
    val name: String?,
    val description: String?,
    val address: String?,
    val openingTime: LocalTime?,
    val closingTime: LocalTime?,
)