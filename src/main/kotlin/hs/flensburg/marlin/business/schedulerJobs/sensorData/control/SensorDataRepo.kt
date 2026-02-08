package hs.flensburg.marlin.business.schedulerJobs.sensorData.control

import de.lambda9.tailwind.jooq.JIO
import de.lambda9.tailwind.jooq.Jooq
import hs.flensburg.marlin.business.schedulerJobs.sensorData.entity.ThingClean
import hs.flensburg.marlin.database.generated.tables.references.GET_OR_CREATE_LOCATION
import hs.flensburg.marlin.database.generated.tables.references.MEASUREMENT
import hs.flensburg.marlin.database.generated.tables.references.MEASUREMENTTYPE
import hs.flensburg.marlin.database.generated.tables.references.SENSOR
import org.jooq.exception.DataAccessException
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

// Distance in meters to combine two sensors into one location
const val DISTANCE_COMBINE_SENSORS = 5 // meters

data class SaveResult(
    val locationId: Long,
    val newMeasurementsSaved: Boolean,
    val timestamp: OffsetDateTime? = null
)

object SensorDataRepo {
    fun saveSensorData(thing: ThingClean): JIO<SaveResult> = Jooq.query {
        // save Sensor
        val sensorId = insertInto(SENSOR)
            .columns(
                SENSOR.ID,
                SENSOR.NAME,
                SENSOR.DESCRIPTION,
                SENSOR.IS_MOVING
            )
            .values(
                thing.id.toLong(),
                thing.name,
                thing.description,
                false
            )
            .onDuplicateKeyUpdate()
            .set(SENSOR.NAME, thing.name)
            .set(SENSOR.DESCRIPTION, thing.description)
            .returning(SENSOR.ID)
            .fetchOne()?.id ?: throw DataAccessException("Failed to save Sensor")

        // Use coordinates from datastreams if they exist, otherwise fallback to thing.location
        val lon = thing.datastreams
            .find { it.observedProperty.name.lowercase() == "longitude" }
            ?.measurements?.lastOrNull()?.result ?: thing.location.first

        val lat = thing.datastreams
            .find { it.observedProperty.name.lowercase() == "latitude" }
            ?.measurements?.lastOrNull()?.result ?: thing.location.second

        // Create/get location based on sensor coordinates
        val locationId = selectFrom(GET_OR_CREATE_LOCATION(lon, lat, DISTANCE_COMBINE_SENSORS))
            .firstOrNull()?.get(0) as Long

        // Split datastreams into location streams and measurement streams
        val (_, measurementStreams) = thing.datastreams.partition { stream ->
            stream.observedProperty.name.lowercase() in setOf("latitude", "longitude")
        }

        // Process Measurements
        var totalInserted = 0
        var time: OffsetDateTime? = null

        measurementStreams.forEach { datastream ->
            val typeId = insertInto(MEASUREMENTTYPE)
                .columns(
                    MEASUREMENTTYPE.ID,
                    MEASUREMENTTYPE.NAME,
                    MEASUREMENTTYPE.DESCRIPTION,
                    MEASUREMENTTYPE.UNIT_NAME,
                    MEASUREMENTTYPE.UNIT_SYMBOL,
                    MEASUREMENTTYPE.UNIT_DEFINITION
                )
                .values(
                    datastream.observedProperty.name.hashCode().toLong(),
                    datastream.observedProperty.name,
                    datastream.observedProperty.description,
                    datastream.unitOfMeasurement.name,
                    datastream.unitOfMeasurement.symbol,
                    datastream.unitOfMeasurement.definition
                )
                .onDuplicateKeyUpdate()
                .set(MEASUREMENTTYPE.NAME, datastream.observedProperty.name)
                .set(MEASUREMENTTYPE.DESCRIPTION, datastream.observedProperty.description)
                .returning(MEASUREMENTTYPE.ID)
                .fetchOne()?.id ?: throw DataAccessException("Failed to save type")

            // save Measurements
            datastream.measurements.forEach { measurement ->
                time = OffsetDateTime.parse(
                    measurement.timestamp,
                    DateTimeFormatter.ISO_OFFSET_DATE_TIME
                )

                totalInserted += insertInto(MEASUREMENT)
                    .columns(
                        MEASUREMENT.SENSOR_ID,
                        MEASUREMENT.TYPE_ID,
                        MEASUREMENT.LOCATION_ID,
                        MEASUREMENT.TIME,
                        MEASUREMENT.VALUE
                    )
                    .values(
                        sensorId,
                        typeId,
                        locationId,
                        time,
                        measurement.result
                    )
                    .onDuplicateKeyIgnore()
                    .execute()
            }
        }
        SaveResult(locationId = locationId, newMeasurementsSaved = totalInserted > 0, timestamp = time)
    }
}