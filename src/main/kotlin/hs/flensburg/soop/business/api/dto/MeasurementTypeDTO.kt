package hs.flensburg.soop.business.api.dto

import kotlinx.serialization.Serializable
import hs.flensburg.soop.database.generated.tables.pojos.Measurementtype

@Serializable
data class MeasurementTypeDTO(
    val id: Long,
    val name: String,
    val description: String?,
    val unitName: String?,
    val unitSymbol: String?,
    val unitDefinition: String?
)

fun Measurementtype.toMeasurementTypeDTO() = MeasurementTypeDTO(
    id = this.id ?: 0L,
    name = this.name ?: "",
    description = this.description,
    unitName = this.unitName,
    unitSymbol = this.unitSymbol,
    unitDefinition = this.unitDefinition
)

