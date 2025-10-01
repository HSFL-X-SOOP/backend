package hs.flensburg.marlin.business.schedulerJobs.sensorData.control

import de.lambda9.tailwind.jooq.Jooq
import hs.flensburg.marlin.business.App
import hs.flensburg.marlin.business.schedulerJobs.sensorData.entity.ThingClean
import hs.flensburg.marlin.database.generated.tables.references.MEASUREMENT
import hs.flensburg.marlin.database.generated.tables.references.MEASUREMENTTYPE
import hs.flensburg.marlin.database.generated.tables.references.SENSOR
import org.jooq.exception.DataAccessException
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

object SensorDataRepo {
    fun saveSensorData(thing: ThingClean): App<DataAccessException, Unit> = Jooq.query {
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
            .fetchOne()?.id ?: throw DataAccessException("Fehler beim Speichern des Sensors")

        // create postgis coordinates with nativ sql
        // ST_DWithin(geographyA, geographyB, distance_in_meters)
        val locationQuery = """
    WITH existing_location AS (
        SELECT id FROM marlin.location 
        WHERE ST_DWithin(coordinates, ST_SetSRID(ST_MakePoint(?, ?), 4326)::geography, 5)
        LIMIT 1
    ),
    new_location AS (
        INSERT INTO marlin.location (coordinates) 
        SELECT ST_SetSRID(ST_MakePoint(?, ?), 4326)
        WHERE NOT EXISTS (SELECT 1 FROM existing_location)
        RETURNING id
    )
    SELECT id FROM new_location
    UNION ALL
    SELECT id FROM existing_location
""".trimIndent()

        // First create/get location ID for the original position
        val originalLocationId = resultQuery(locationQuery,
            thing.location.first,  // original longitude
            thing.location.second, // original latitude
            thing.location.first,  // original longitude
            thing.location.second  // original latitude
        ).fetchOne()?.get(0) as Long? ?: throw DataAccessException("Fehler beim Speichern der originalen Location")

        // Split datastreams into location streams and measurement streams
        val (locationStreams, measurementStreams) = thing.datastreams.partition { stream ->
            stream.observedProperty.name.lowercase() in setOf("latitude", "longitude")
        }

        // Then check if we have updated coordinates from datastreams
        val updatedLocationId = if (locationStreams.size == 2) {
            val lat = locationStreams.find { it.observedProperty.name.lowercase() == "latitude" }
                ?.measurements?.lastOrNull()?.result
            val lon = locationStreams.find { it.observedProperty.name.lowercase() == "longitude" }
                ?.measurements?.lastOrNull()?.result
            
            if (lat != null && lon != null) {
                resultQuery(locationQuery, lon, lat, lon, lat)
                    .fetchOne()?.get(0) as Long?
            } else null
        } else null


        // Use the updated location if available, otherwise use the original
        val locationId = updatedLocationId ?: originalLocationId

        // Process only the non-location datastreams
        measurementStreams.forEach { datastream ->
            // save MeasurementType
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
                .fetchOne()?.id ?: throw DataAccessException("Fehler beim Speichern des MeasurementType")

            // save Measurements
            datastream.measurements.forEach { measurement ->
                val time = OffsetDateTime.parse(measurement.timestamp,
                    DateTimeFormatter.ISO_OFFSET_DATE_TIME)

                insertInto(MEASUREMENT)
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
    }
}