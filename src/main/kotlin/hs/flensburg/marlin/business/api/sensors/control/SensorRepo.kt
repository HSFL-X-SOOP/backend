package hs.flensburg.marlin.business.api.sensors.control

import de.lambda9.tailwind.jooq.JIO
import de.lambda9.tailwind.jooq.Jooq
import hs.flensburg.marlin.business.api.location.entity.GeoPoint
import hs.flensburg.marlin.business.api.sensors.entity.EnrichedMeasurementDTO
import hs.flensburg.marlin.business.api.sensors.entity.LocationWithLatestMeasurementsDTO
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
import hs.flensburg.marlin.database.generated.tables.references.POTENTIAL_SENSOR
import hs.flensburg.marlin.database.generated.tables.references.SENSOR
import java.time.OffsetDateTime

object SensorRepo {

    fun fetchAllSensors(): JIO<List<Sensor>> = Jooq.query {
        selectFrom(SENSOR)
            .orderBy(SENSOR.ID.asc())
            .fetchInto(Sensor::class.java)
    }

    fun fetchAllMeasurementTypes(): JIO<List<Measurementtype>> = Jooq.query {
        selectFrom(MEASUREMENTTYPE).fetchInto(Measurementtype::class.java)
    }

    fun fetchAllLocations(): JIO<List<Location>> = Jooq.query {
        selectFrom(LOCATION)
            .orderBy(LOCATION.ID.asc())
            .fetchInto(Location::class.java)
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
                        coordinates = GeoPoint(
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
        timeRange: String,
        timezone: String,
        units: String
    ): JIO<LocationWithLatestMeasurementsDTO?> = Jooq.query {

        val intervalCondition = when (timeRange.lowercase()) {
            "48h" -> "m.time >= NOW() - INTERVAL '48 hours'"
            "7d" -> "m.time >= NOW() - INTERVAL '7 days'"
            "30d" -> "m.time >= NOW() - INTERVAL '30 days'"
            "90d" -> "m.time >= NOW() - INTERVAL '90 days'"
            "180d" -> "m.time >= NOW() - INTERVAL '180 days'"
            "1y" -> "m.time >= NOW() - INTERVAL '1 years'"
            else -> "m.time >= NOW() - INTERVAL '24 hours'"
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
                    coordinates = GeoPoint(
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
        )

        grouped.entries.firstOrNull()?.let { (location, measurements) ->
            LocationWithLatestMeasurementsDTO(location, measurements)
        }
    }

    fun fetchLocationByIDWithMeasurementsWithinTimespanFAST(
        locationId: Long,
        timeRange: String,
        timezone: String,
        units: String
    ): JIO<LocationWithLatestMeasurementsDTO?> = Jooq.query {

        val (useRaw, bucketWidth, intervalCondition) = when (timeRange.lowercase()) {
            "24h" -> Triple(true, null, "m.time >= NOW() - INTERVAL '24 hours'")
            "48h" -> Triple(true, null, "m.time >= NOW() - INTERVAL '48 hours'")
            "7d" -> Triple(false, "2 hours", "m.time >= NOW() - INTERVAL '7 days'")
            "30d" -> Triple(false, "6 hours", "m.time >= NOW() - INTERVAL '30 days'")
            "90d" -> Triple(false, "12 hours", "m.time >= NOW() - INTERVAL '90 days'")
            "180d" -> Triple(false, "1 day", "m.time >= NOW() - INTERVAL '180 days'")
            "1y" -> Triple(false, "2 day", "m.time >= NOW() - INTERVAL '1 years'")
            else -> Triple(true, null, "m.time >= NOW() - INTERVAL '24 hours'")
        }

        val sql =
            if (useRaw)
            // Fetch with raw sql
                """
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
            else
            // fetch with time_buckets to get average
                """
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
          public.time_bucket(INTERVAL '$bucketWidth', m.time) AS meas_time,
          ROUND(AVG(m.value)::numeric, 2) AS meas_value
        FROM marlin.measurement AS m
        JOIN marlin.location l ON m.location_id = l.id
        JOIN marlin.sensor s ON m.sensor_id = s.id
        JOIN marlin.measurementtype mt ON m.type_id = mt.id
        WHERE m.location_id = $locationId
          AND $intervalCondition
        GROUP BY l.id, s.id, mt.id, meas_time
        ORDER BY meas_time DESC, mt.id;
    """.trimIndent()

        val grouped = resultQuery(sql).fetchGroups(
            { rec ->
                println(rec)
                LocationDTO(
                    id = rec.get("loc_id", Long::class.java)!!,
                    name = rec.get("loc_name", String::class.java),
                    coordinates = GeoPoint(
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
        )

        grouped.entries.firstOrNull()?.let { (location, measurements) ->
            LocationWithLatestMeasurementsDTO(location, measurements)
        }
    }

    fun countAllActiveSensors(): JIO<Int> = Jooq.query {
        selectCount()
            .from(POTENTIAL_SENSOR)
            .where(POTENTIAL_SENSOR.IS_ACTIVE.eq(true))
            .fetchOneInto(Int::class.java) ?: 0
    }

    fun countAllMeasurementsToday(): JIO<Int> = Jooq.query {
        selectCount()
            .from(MEASUREMENT)
            .where(
                MEASUREMENT.TIME.greaterOrEqual(
                    OffsetDateTime.now().toLocalDate().atStartOfDay().atOffset(OffsetDateTime.now().offset)
                )
            )
            .fetchOneInto(Int::class.java) ?: 0
    }

}