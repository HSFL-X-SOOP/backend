package hs.flensburg.marlin.business.api.sensors.entity

import kotlinx.serialization.Serializable

@Serializable
data class UnitsWithLocationWithBoxesDTO(
    val units: MeasurementUnitsDTO,
    val locationWithBoxes: List<LocationWithBoxesDTO>
)

fun mapToUnitsWithLocationWithBoxesDTO(rawLocations: List<LocationWithLatestMeasurementsDTO>): UnitsWithLocationWithBoxesDTO {
    val unitMap = rawLocations
        .flatMap { it.latestMeasurements }
        .associate { measurement ->
            measurement.measurementType.name to measurement.measurementType.unitSymbol
        }

    return UnitsWithLocationWithBoxesDTO(
        units = MeasurementUnitsDTO(
            unitMap["Temperature, water"] ?: "",
            unitMap["Wave Height"] ?: "",
            unitMap["Tide"] ?: "",
            unitMap["Standard deviation"] ?: "",
            unitMap["Battery, voltage"] ?: "",
            unitMap["Temperature, air"] ?: "",
            unitMap["Wind speed"] ?: "",
            unitMap["Wind direction"] ?: "",
            unitMap["Wind speed, gust"] ?: "",
            unitMap["Wind direction, gust"] ?: "",
            unitMap["Humidity, relative"] ?: "",
            unitMap["Station pressure"] ?: ""
        ),
        locationWithBoxes = rawLocations.map { it.mapToLocationWithBoxesDTO() }
    )
}


