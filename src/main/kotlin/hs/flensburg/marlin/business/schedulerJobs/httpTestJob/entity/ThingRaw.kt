package hs.flensburg.marlin.business.schedulerJobs.httpTestJob.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive

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
data class ObservationRaw(val phenomenonTime: String, val result: JsonElement)

// ==== Raw to Clean ===


// Remove the entire Datastream if it doesnt measure a numeric value, such as Timestamp
fun ThingRaw.toClean(): ThingClean {
    val coords = Locations.firstOrNull()?.location?.coordinates ?: listOf(0.0, 0.0)
    return ThingClean(
        id = id,
        name = name,
        description = description,
        location = Pair(coords[0], coords[1]),
        datastreams = Datastreams.mapNotNull { it.toClean() }
    )
}
// Remove Datastream if it has no Measuremnt as Double
fun DatastreamRaw.toClean(): DatastreamClean? {
    val numericMeasurements = observations.mapNotNull { it.toClean() }
    return if (numericMeasurements.isEmpty()) null
    else DatastreamClean(
        name = name,
        description = description,
        unitOfMeasurement = unitOfMeasurement,
        totalTimeSpan = totalTimeSpan,
        sensor = sensor,
        observedProperty = observedProperty,
        measurements = numericMeasurements
    )
}
// Remove Values that are not a Double, like Timestamp
fun ObservationRaw.toClean(): ObservationClean? {
    val resultDouble = when (result) {
        is JsonElement -> result.jsonPrimitive.doubleOrNull
        else -> null
    }

    return resultDouble?.let { ObservationClean(timestamp = phenomenonTime, result = it) }
}
