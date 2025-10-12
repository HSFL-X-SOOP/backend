package hs.flensburg.marlin.business.api.potentialSensors.entity

import hs.flensburg.marlin.database.generated.tables.pojos.PotentialSensor
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PotentialSensorDTO(
    val id: Long?,
    val name: String?,
    val description: String?,
    val isActive: Boolean?
)

fun PotentialSensor.toDTO() = PotentialSensorDTO(
    id = this.id,
    name = this.name,
    description = this.description,
    isActive = this.isActive
)
