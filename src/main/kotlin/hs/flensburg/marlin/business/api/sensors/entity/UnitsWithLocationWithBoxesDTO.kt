package hs.flensburg.marlin.business.api.sensors.entity

import hs.flensburg.marlin.business.api.units.entity.UnitsDTO
import kotlinx.serialization.Serializable

@Serializable
data class UnitsWithLocationWithBoxesDTO (
    val units: UnitsDTO,
    val locationWithBoxes: List<LocationWithBoxesDTO>
)

fun mapToUnitsWithLocationWithBoxesDTO(rawLocations: List<LocationWithLatestMeasurementsDTO>): UnitsWithLocationWithBoxesDTO {
    return UnitsWithLocationWithBoxesDTO(
        units = UnitsDTO(
            getUnitFromRawLocations(rawLocations,"Temperature, water"),
            getUnitFromRawLocations(rawLocations,"Wave Height"),
            getUnitFromRawLocations(rawLocations,"Tide"),
            getUnitFromRawLocations(rawLocations,"Standard deviation"),
            getUnitFromRawLocations(rawLocations,"Battery, voltage"),
            getUnitFromRawLocations(rawLocations,"Temperature, air"),
            getUnitFromRawLocations(rawLocations,"Wind speed"),
            getUnitFromRawLocations(rawLocations,"Wind direction"),
            getUnitFromRawLocations(rawLocations,"Wind speed, gust"),
            getUnitFromRawLocations(rawLocations,"Wind direction, gust"),
            getUnitFromRawLocations(rawLocations,"Humidity, relative"),
            getUnitFromRawLocations(rawLocations,"Station pressure")
        ),
        locationWithBoxes = rawLocations.map { it.mapToLocationWithBoxesDTO() }
    )
}

fun getUnitFromRawLocations(rawLocations: List<LocationWithLatestMeasurementsDTO>, unitName: String): String {
    return rawLocations.map { (_, measurements) ->
        val unit = measurements.find { it.measurementType.name == unitName}?.measurementType?.unitSymbol
        unit
    }.firstNotNullOf { it }
}


