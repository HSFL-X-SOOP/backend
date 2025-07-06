package hs.flensburg.soop.business.api

import de.lambda9.tailwind.jooq.Jooq
import hs.flensburg.soop.business.App
import hs.flensburg.soop.business.api.dto.GeoPointDTO
import hs.flensburg.soop.business.api.dto.LocationDTO
import hs.flensburg.soop.business.api.dto.LocationWithLatestMeasurementsDTO
import hs.flensburg.soop.business.api.dto.MeasurementDTO
import hs.flensburg.soop.business.api.dto.toMeasurementDTO
import hs.flensburg.soop.database.generated.tables.pojos.Location
import hs.flensburg.soop.database.generated.tables.pojos.Measurement
import hs.flensburg.soop.database.generated.tables.pojos.Measurementtype
import hs.flensburg.soop.database.generated.tables.pojos.Sensor
import hs.flensburg.soop.database.generated.tables.references.LOCATION
import hs.flensburg.soop.database.generated.tables.references.MEASUREMENT
import hs.flensburg.soop.database.generated.tables.references.MEASUREMENTTYPE
import hs.flensburg.soop.database.generated.tables.references.SENSOR
import kotlinx.datetime.LocalDateTime
import org.jooq.exception.DataAccessException
import org.jooq.impl.DSL
import java.time.OffsetDateTime


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

fun getLocationsWithLatestMeasurements(): App<DataAccessException, List<LocationWithLatestMeasurementsDTO>> = Jooq.query {
    val sql = """
        WITH latest AS (
          SELECT DISTINCT ON (m.location_id, m.type_id)
            m.sensor_id,
            m.type_id,
            m.location_id,
            m.time,
            m.value
          FROM soop.measurement AS m
          ORDER BY m.location_id, m.type_id, m.time DESC
        )
        SELECT
          l.id AS loc_id,
          l.name AS loc_name,
          ST_Y(l.coordinates::geometry) AS latitude,
          ST_X(l.coordinates::geometry) AS longitude,
          lm.sensor_id,
          lm.type_id,
          lm.time AS meas_time,
          lm.value AS meas_value
        FROM latest AS lm
        JOIN soop.location AS l ON lm.location_id = l.id
        ORDER BY l.id;
      """.trimIndent()

    resultQuery(sql).fetchGroups(
        // Key = LocationDTO
        {
            val id = it.get("loc_id", Long::class.java)!!
            LocationDTO(
                id = id,
                name = it.get("loc_name", String::class.java),
                coordinates = GeoPointDTO(
                    it.get("latitude", Double::class.java)!!,
                    it.get("longitude", Double::class.java)!!
                )
            )
        },
        // Value = Measurement → then .toMeasurementDTO()
        { rec ->
            Measurement(
                sensorId = rec.get("sensor_id", Long::class.java),
                typeId = rec.get("type_id", Long::class.java),
                locationId = rec.get("loc_id", Long::class.java),
                time = rec.get("meas_time", OffsetDateTime::class.java),
                value = rec.get("meas_value", Double::class.java)
            ).toMeasurementDTO()
        }
    ).map { (locationDto, measurements) ->
        LocationWithLatestMeasurementsDTO(location = locationDto, latestMeasurements = measurements)
    }
}

