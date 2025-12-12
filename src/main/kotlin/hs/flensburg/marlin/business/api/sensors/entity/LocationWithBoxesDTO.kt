package hs.flensburg.marlin.business.api.sensors.entity

import hs.flensburg.marlin.business.api.sensors.entity.boxes.BoxDTO
import hs.flensburg.marlin.business.api.sensors.entity.boxes.mapSensorToBoxDTO
import hs.flensburg.marlin.business.api.sensors.entity.raw.LocationDTO
import kotlinx.serialization.Serializable

@Serializable
data class LocationWithBoxesDTO(
    val location: LocationDTO,
    val boxes: List<BoxDTO>
)

fun LocationWithLatestMeasurementsDTO.toLocationWithBoxesDTO(): LocationWithBoxesDTO {
    val boxes = latestMeasurements
        .groupBy { it.sensor.id }
        .map { (_, measurements) ->
            val sensor = measurements.first().sensor
            mapSensorToBoxDTO(sensor, measurements)
        }

    return LocationWithBoxesDTO(
        location = location,
        boxes = boxes
    )
}
