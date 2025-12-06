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
import hs.flensburg.marlin.database.generated.tables.pojos.Location
import hs.flensburg.marlin.database.generated.tables.pojos.Measurement
import hs.flensburg.marlin.database.generated.tables.pojos.Measurementtype
import hs.flensburg.marlin.database.generated.tables.pojos.Sensor
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
            selectFrom(LATEST_MEASUREMENTS_VIEW)
                .fetchAndMapToListOfLocationWithLatestMeasurementsDTO(timezone)
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
            .fetchAndMapToListOfLocationWithLatestMeasurementsDTO(timezone)
            .firstOrNull()
    }


    private fun <R : Record> ResultQuery<R>.fetchAndMapToListOfLocationWithLatestMeasurementsDTO(
        timezone: String
    ): List<LocationWithLatestMeasurementsDTO> {

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
                    else -> throw IllegalStateException("No timestamp found")
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
        // Transform the Map into a List
        return grouped.map { (location, measurements) ->
            LocationWithLatestMeasurementsDTO(location, measurements)
        }
    }
}