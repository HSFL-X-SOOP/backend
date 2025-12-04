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
import hs.flensburg.marlin.database.generated.tables.pojos.*
import hs.flensburg.marlin.database.generated.tables.references.*
import org.jooq.Record
import org.jooq.ResultQuery
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

    fun fetchLocationsWithLatestMeasurements(timezone: String): JIO<List<LocationWithLatestMeasurementsDTO>> =
        Jooq.query {
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

                    EnrichedMeasurementDTO(
                        sensor = sensor,
                        measurementType = type,
                        time = localTime,
                        value = rec.get("meas_value", Double::class.java)!!
                    )
                }
            ).map { (location, enrichedMeasurements) ->
                LocationWithLatestMeasurementsDTO(location, enrichedMeasurements)
            }
        }

    fun fetchLocationByIDWithMeasurementsWithinTimespan(
        locationId: Long,
        timeRange: String,
        timezone: String
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

    fun getLatestMeasurementTimeEnriched(
        locationId: Long,
        timeRange: String,
        timezone: String
    ): JIO<LocationWithLatestMeasurementsDTO?> = Jooq.query {
        selectFrom(GET_ENRICHED_MEASUREMENTS(timeRange, locationId, null, null))
            .fetchAndMapToLocationWithLatestMeasurementsDTO(timezone)
    }


    private fun <R : Record> ResultQuery<R>.fetchAndMapToLocationWithLatestMeasurementsDTO(
        timezone: String
    ): LocationWithLatestMeasurementsDTO? {

        val grouped = this.fetchGroups(

            { rec: R ->
                LocationDTO(
                    id = rec.get("location_id", Long::class.javaObjectType)!!,
                    name = rec.get("location_name", String::class.java),
                    coordinates = GeoPoint(
                        lat = rec.get("latitude", Double::class.javaObjectType)!!,
                        lon = rec.get("longitude", Double::class.javaObjectType)!!
                    )
                )
            },

            { rec: R ->
                val sensor = Sensor(
                    id = rec.get("sensor_id", Long::class.javaObjectType),
                    name = rec.get("sensor_name", String::class.java),
                    description = rec.get("sensor_description", String::class.java),
                    isMoving = if (rec.field("sensor_is_moving") != null)
                        rec.get("sensor_is_moving", Boolean::class.javaObjectType)
                    else false
                ).toSensorDTO()

                val type = Measurementtype(
                    id = rec.get("type_id", Long::class.javaObjectType),
                    name = rec.get("type_name", String::class.java),
                    description = rec.get("type_description", String::class.java),
                    unitName = rec.get("unit_name", String::class.java),
                    unitSymbol = rec.get("unit_symbol", String::class.java),
                    unitDefinition = rec.get("unit_definition", String::class.java)
                ).toMeasurementTypeDTO()

                // 1. Safe Time Retrieval (View has 'meas_time', Routine has 'bucket')
                val time: OffsetDateTime = when {
                    rec.field("meas_time") != null -> rec.get("meas_time", OffsetDateTime::class.java)
                    rec.field("bucket") != null -> rec.get("bucket", OffsetDateTime::class.java)
                    else -> throw IllegalStateException("Record has neither 'meas_time' nor 'bucket'")
                }

                // 2. Safe Value Retrieval (View has 'meas_value', Routine has 'avg')
                val value: Double = when {
                    rec.field("meas_value") != null -> rec.get("meas_value", Double::class.javaObjectType)
                    rec.field("avg") != null -> rec.get("avg", Double::class.javaObjectType)
                    else -> 0.0
                } ?: 0.0

                // TODO: handle: min, max, stddev
                EnrichedMeasurementDTO(
                    sensor = sensor,
                    measurementType = type,
                    time = TimezonesService.toLocalDateTimeInZone(time, timezone),
                    value = value
                )
            }
        )

        return grouped.entries.firstOrNull()?.let { (location, measurements) ->
            LocationWithLatestMeasurementsDTO(location, measurements)
        }
    }
}