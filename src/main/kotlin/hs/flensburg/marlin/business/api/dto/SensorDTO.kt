package hs.flensburg.marlin.business.api.dto

import kotlinx.serialization.Serializable
import hs.flensburg.marlin.database.generated.tables.pojos.Sensor

@Serializable
data class SensorDTO(
    val id: Long?,
    val name: String?,
    val description: String?,
    val isMoving: Boolean?
)

// Mapping-Funktion
fun Sensor.toSensorDTO() = SensorDTO(
    id = this.id,
    name = this.name,
    description = this.description,
    isMoving = this.isMoving
)