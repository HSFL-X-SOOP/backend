package hs.flensburg.marlin.business.api.sensors.control

import de.lambda9.tailwind.jooq.JIO
import de.lambda9.tailwind.jooq.Jooq
import hs.flensburg.marlin.business.api.sensors.entity.EnrichedMeasurementDTO
import hs.flensburg.marlin.business.api.sensors.entity.LocationWithLatestMeasurementsDTO
import hs.flensburg.marlin.business.api.sensors.entity.raw.GeoPointDTO
import hs.flensburg.marlin.business.api.sensors.entity.raw.LocationDTO
import hs.flensburg.marlin.business.api.sensors.entity.raw.toMeasurementTypeDTO
import hs.flensburg.marlin.business.api.sensors.entity.raw.toSensorDTO
import hs.flensburg.marlin.business.api.timezones.boundary.TimezonesService
import hs.flensburg.marlin.business.api.units.boundary.UnitsService
import hs.flensburg.marlin.database.generated.tables.pojos.Location
import hs.flensburg.marlin.database.generated.tables.pojos.Measurement
import hs.flensburg.marlin.database.generated.tables.pojos.Measurementtype
import hs.flensburg.marlin.database.generated.tables.pojos.Sensor
import hs.flensburg.marlin.database.generated.tables.references.LOCATION
import hs.flensburg.marlin.database.generated.tables.references.MEASUREMENT
import hs.flensburg.marlin.database.generated.tables.references.MEASUREMENTTYPE
import hs.flensburg.marlin.database.generated.tables.references.SENSOR
import org.jooq.impl.DSL
import java.time.OffsetDateTime

object SensorRepo {

    fun fetchAllSensors(): JIO<List<Sensor>> = Jooq.query {
        selectFrom(SENSOR).fetchInto(Sensor::class.java)
    }

    fun fetchAllMeasurementTypes(): JIO<List<Measurementtype>> = Jooq.query {
        selectFrom(MEASUREMENTTYPE).fetchInto(Measurementtype::class.java)
    }

    fun fetchAllLocations(): JIO<List<Location>> = Jooq.query {
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
                    // combine latitude and longitude into a Pair
                    coordinates = it.get("latitude", Double::class.java)?.let { lat ->
                        it.get("longitude", Double::class.java)?.let { lon ->
                            Pair(lat, lon)
                        }
                    }
                )
            }
    }

    fun fetchAllMeasurements(): JIO<List<Measurement>> = Jooq.query {
        selectFrom(MEASUREMENT)
            .orderBy(MEASUREMENT.TIME.desc())
            .fetchInto(Measurement::class.java)
    }

    fun fetchLocationsWithLatestMeasurements(timezone: String, units: String): JIO<List<LocationWithLatestMeasurementsDTO>> = Jooq.query {
        // Query to fetch only the newest measurement within the last 2 hours for each location
        val sql = """
        WITH latest AS (
          SELECT DISTINCT ON (m.location_id, m.type_id)
            m.sensor_id,
            m.type_id,
            m.location_id,
            m.time,
            m.value
          FROM marlin.measurement AS m
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
        JOIN marlin.location l ON lm.location_id = l.id
        JOIN marlin.sensor s ON lm.sensor_id = s.id
        JOIN marlin.measurementtype mt ON lm.type_id = mt.id
        
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
                    val localTime = TimezonesService.toLocalDateTimeInZone(time, timezone)

                    val (valueConverted, unit) = UnitsService.convert(rec.get("meas_value", Double::class.java)!!,
                        type, units)

                    type.unitSymbol = unit

                    EnrichedMeasurementDTO(
                        sensor = sensor,
                        measurementType = type,
                        time = localTime,
                        value = valueConverted
                    )
                }
            ).map { (location, enrichedMeasurements) ->
                LocationWithLatestMeasurementsDTO(location, enrichedMeasurements)
            }
    }

    fun fetchLocationByIDWithMeasurementsWithinTimespan(
        locationId: Long,
        timeRange: String, // "today", "week", "month"
        timezone: String
    ): JIO<LocationWithLatestMeasurementsDTO?> = Jooq.query {

        val intervalCondition = when (timeRange.lowercase()) {
            "today" -> "m.time >= date_trunc('day', NOW() AT TIME ZONE '$timezone') AT TIME ZONE '$timezone'"
            "week" -> "m.time >= date_trunc('week', NOW() AT TIME ZONE '$timezone') AT TIME ZONE '$timezone'"
            "month" -> "m.time >= date_trunc('month', NOW() AT TIME ZONE '$timezone') AT TIME ZONE '$timezone'"
            else -> "m.time >= (NOW() AT TIME ZONE '$timezone') - INTERVAL '1 day'"
        }

        val sql = """
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

          m.time AS meas_time,
          m.value AS meas_value

        FROM marlin.measurement AS m
        JOIN marlin.location l ON m.location_id = l.id
        JOIN marlin.sensor s ON m.sensor_id = s.id
        JOIN marlin.measurementtype mt ON m.type_id = mt.id

        WHERE m.location_id = $locationId
          AND $intervalCondition
        ORDER BY m.time DESC;
    """.trimIndent()

        val grouped = resultQuery(sql).fetchGroups(
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
                val localTime = TimezonesService.toLocalDateTimeInZone(time, timezone)

                EnrichedMeasurementDTO(
                    sensor = sensor,
                    measurementType = type,
                    time = localTime,
                    value = rec.get("meas_value", Double::class.java)!!
                )


            }
        )

        grouped.entries.firstOrNull()?.let { (location, measurements) ->
            LocationWithLatestMeasurementsDTO(location, measurements)
        }
    }
}