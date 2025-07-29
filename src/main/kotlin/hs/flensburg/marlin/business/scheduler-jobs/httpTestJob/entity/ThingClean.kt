package hs.flensburg.marlin.business.`scheduler-jobs`.httpTestJob.entity


// ==== Clean Data Models ====
data class ThingClean(
    val id: Int,
    val name: String,
    val description: String,
    val location: Pair<Double, Double>, // lon, lat
    val datastreams: List<DatastreamClean>
)

data class DatastreamClean(
    val name: String,
    val description: String,
    val unitOfMeasurement: UnitOfMeasurement,
    val totalTimeSpan: String,
    val sensor: Sensor,
    val observedProperty: ObservedProperty,
    val measurements: List<ObservationClean>
)

data class ObservationClean(val timestamp: String, val result: Double)
