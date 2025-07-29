package hs.flensburg.soop.business.api

import de.lambda9.tailwind.jooq.Jooq
import hs.flensburg.soop.business.App
import hs.flensburg.soop.business.api.dto.EnrichedMeasurementDTO
import hs.flensburg.soop.business.api.dto.GeoPointDTO
import hs.flensburg.soop.business.api.dto.LocationDTO
import hs.flensburg.soop.business.api.dto.LocationWithLatestMeasurementsDTO
import hs.flensburg.soop.business.api.dto.toMeasurementTypeDTO
import hs.flensburg.soop.business.api.dto.toSensorDTO
import hs.flensburg.soop.database.generated.tables.pojos.Location
import hs.flensburg.soop.database.generated.tables.pojos.Measurement
import hs.flensburg.soop.database.generated.tables.pojos.Measurementtype
import hs.flensburg.soop.database.generated.tables.pojos.Sensor
import hs.flensburg.soop.database.generated.tables.references.LOCATION
import hs.flensburg.soop.database.generated.tables.references.MEASUREMENT
import hs.flensburg.soop.database.generated.tables.references.MEASUREMENTTYPE
import hs.flensburg.soop.database.generated.tables.references.SENSOR
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jooq.exception.DataAccessException
import org.jooq.impl.DSL
import java.time.OffsetDateTime
import kotlin.time.ExperimentalTime
import kotlin.time.toKotlinInstant


fun getAllSensorsFromDB(): App<DataAccessException, List<Sensor>> = Jooq.query {
    selectFrom(SENSOR).fetchInto(Sensor::class.java)
}

fun getAllMeasurementTypesFromDB(): App<DataAccessException, List<Measurementtype>> = Jooq.query {
    selectFrom(MEASUREMENTTYPE).fetchInto(Measurementtype::class.java)
}

fun getAllLocationsFromDB(): App<DataAccessException, List<Location>> = Jooq.query {
    select(
        LOCATION.ID,
        LOCATION.NAME,
        // Use PostGIS functions to extract latitude and longitude from coordinates
        DSL.field("ST_Y(coordinates::geometry)", Double::class.java).`as`("latitude"),
        DSL.field("ST_X(coordinates::geometry)", Double::class.java).`as`("longitude")
    ).from(LOCATION)
        .fetch {
            Location(
                id = it[LOCATION.ID],
                name = it[LOCATION.NAME],
                // compine latitude and longitude into a Pair
                coordinates = it.get("latitude", Double::class.java)?.let { lat ->
                    it.get("longitude", Double::class.java)?.let { lon ->
                        Pair(lat, lon)
                    }
                }
            )
        }
}

fun getAllMeasurementsFromDB(): App<DataAccessException, List<Measurement>> = Jooq.query {
    selectFrom(MEASUREMENT).fetchInto(Measurement::class.java)
}

@OptIn(ExperimentalTime::class)
fun getLocationsWithLatestMeasurements(): App<DataAccessException, List<LocationWithLatestMeasurementsDTO>> = Jooq.query {
    // Query to fetch only the newest measurement within the last 2 hours for each location
    val sql = """
    WITH latest AS (
      SELECT DISTINCT ON (m.location_id, m.type_id)
        m.sensor_id,
        m.type_id,
        m.location_id,
        m.time,
        m.value
      FROM soop.measurement AS m
      WHERE m.time >= NOW() - INTERVAL '2 hour' -- last 2 hours
      ORDER BY m.location_id, m.type_id, m.time DESC
    )
    SELECT
      l.id AS loc_id,
      l.name AS loc_name,
      ST_Y(l.coordinates::geometry) AS latitude,
      ST_X(l.coordinates::geometry) AS longitude,

      s.id AS sensor_id,
      s.name AS sensor_name,
      s.description AS sensor_description,
      s.is_moving AS sensor_is_moving,

      mt.id AS type_id,
      mt.name AS type_name,
      mt.description AS type_description,
      mt.unit_name,
      mt.unit_symbol,
      mt.unit_definition,

      lm.time AS meas_time,
      lm.value AS meas_value

    FROM latest AS lm
    JOIN soop.location l ON lm.location_id = l.id
    JOIN soop.sensor s ON lm.sensor_id = s.id
    JOIN soop.measurementtype mt ON lm.type_id = mt.id

    ORDER BY l.id;
  """.trimIndent()

    resultQuery(sql).fetchGroups(
        { rec ->
            LocationDTO(
                id = rec.get("loc_id", Long::class.java)!!,
                name = rec.get("loc_name", String::class.java),
                coordinates = GeoPointDTO(
                    lat = rec.get("latitude", Double::class.java)!!,
                    lon = rec.get("longitude", Double::class.java)!!
                )
            )
        },
        { rec ->
            val sensor = Sensor(
                id = rec.get("sensor_id", Long::class.java),
                name = rec.get("sensor_name", String::class.java),
                description = rec.get("sensor_description", String::class.java),
                isMoving = rec.get("sensor_is_moving", Boolean::class.java)
            ).toSensorDTO()

            val type = Measurementtype(
                id = rec.get("type_id", Long::class.java),
                name = rec.get("type_name", String::class.java),
                description = rec.get("type_description", String::class.java),
                unitName = rec.get("unit_name", String::class.java),
                unitSymbol = rec.get("unit_symbol", String::class.java),
                unitDefinition = rec.get("unit_definition", String::class.java)
            ).toMeasurementTypeDTO()

            val time = rec.get("meas_time", OffsetDateTime::class.java)
                .toInstant()?.toKotlinInstant()?.toLocalDateTime(TimeZone.UTC)

            EnrichedMeasurementDTO(
                sensor = sensor,
                measurementType = type,
                time = time,
                value = rec.get("meas_value", Double::class.java)!!
            )
        }
    ).map { (location, enrichedMeasurements) ->
        LocationWithLatestMeasurementsDTO(location, enrichedMeasurements)
    }

}

