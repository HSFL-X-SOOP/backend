package hs.flensburg.marlin.business.schedulerJobs.potentialSensors.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PotentialSensor(
    @SerialName("@iot.id") val id: Int,
    val name: String,
    val description: String,
    val isSensor: Boolean = false
)

@Serializable
data class FrostResponse<T>(
    val value: List<T>
)
