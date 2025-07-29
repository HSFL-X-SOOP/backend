package hs.flensburg.marlin.business.`scheduler-jobs`.httpTestJob.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ==== RAW DATA MODELS ====

@Serializable
data class ThingRaw(
    @SerialName("@iot.id") val id: Int,
    val name: String,
    val description: String,
    val Locations: List<LocationRaw>,
    val Datastreams: List<DatastreamRaw>
)

@Serializable
data class LocationRaw(
    val location: GeoJsonPoint
)

@Serializable
data class GeoJsonPoint(
    val coordinates: List<Double> // [lon, lat]
)

@Serializable
data class DatastreamRaw(
    val name: String,
    val description: String,
    val unitOfMeasurement: UnitOfMeasurement,
    @SerialName("phenomenonTime") val totalTimeSpan: String,
    @SerialName("Sensor") val sensor: Sensor,
    @SerialName("ObservedProperty") val observedProperty: ObservedProperty,
    @SerialName("Observations") val observations: List<ObservationRaw>
)

@Serializable
data class UnitOfMeasurement(val name: String, val symbol: String, val definition: String)
@Serializable
data class Sensor(val name: String, val description: String, val metadata: String)
@Serializable
data class ObservedProperty(val name: String, val description: String)
@Serializable
data class ObservationRaw(val phenomenonTime: String, val result: Double)

// ==== Raw to Clean ===



fun ThingRaw.toClean(): ThingClean {
    val coords = Locations.firstOrNull()?.location?.coordinates ?: listOf(0.0, 0.0)
    return ThingClean(
        id = id,
        name = name,
        description = description,
        location = Pair(coords[0], coords[1]),
        datastreams = Datastreams.map { it.toClean() }
    )
}

fun DatastreamRaw.toClean(): DatastreamClean {
    return DatastreamClean(
        name = name,
        description = description,
        unitOfMeasurement = unitOfMeasurement,
        totalTimeSpan = totalTimeSpan,
        sensor = sensor,
        observedProperty = observedProperty,
        measurements = observations.map { it.toClean() }
    )
}

fun ObservationRaw.toClean(): ObservationClean {
    return ObservationClean(timestamp = phenomenonTime, result = result)
}
