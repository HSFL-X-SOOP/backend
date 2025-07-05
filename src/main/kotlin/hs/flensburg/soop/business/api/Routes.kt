package hs.flensburg.soop.business.api

import de.lambda9.tailwind.core.KIO.Companion.unsafeRunSync
import de.lambda9.tailwind.jooq.Jooq
import hs.flensburg.soop.business.App
import hs.flensburg.soop.business.JEnv
import hs.flensburg.soop.database.generated.tables.pojos.Location
import hs.flensburg.soop.database.generated.tables.references.LOCATION
import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import org.jooq.exception.DataAccessException
import hs.flensburg.soop.database.generated.tables.references.SENSOR
import hs.flensburg.soop.database.generated.tables.pojos.Sensor
import hs.flensburg.soop.database.generated.tables.references.MEASUREMENTTYPE
import hs.flensburg.soop.database.generated.tables.pojos.Measurementtype
import kotlin.text.get


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
        org.jooq.impl.DSL.field("ST_Y(coordinates::geometry)", Double::class.java).`as`("latitude"),
        org.jooq.impl.DSL.field("ST_X(coordinates::geometry)", Double::class.java).`as`("longitude")
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
/*
fun getLocationsWithLatestMeasurements(): App<DataAccessException, List<Location>> = Jooq.query {
    result = select(
            Location.ID,
            Location.NAME,
            Measurement.ID.`as`("measurement_id"),
            Measurement.VALUE,
            Measurement.TIMESTAMP,
            MeasurementType.ID.`as`("type_id"),
            MeasurementType.NAME.`as`("type_name"),
            Sensor.ID.`as`("sensor_id"),
            Sensor.NAME.`as`("sensor_name")
        )
        .from(Location)
        .leftJoin(Sensor).on(Sensor.LOCATION_ID.eq(Location.ID))
        .leftJoin(Measurement).on(Measurement.SENSOR_ID.eq(Sensor.ID))
        .leftJoin(MeasurementType).on(Measurement.MEASUREMENT_TYPE_ID.eq(MeasurementType.ID))
        .where(
            Measurement.TIMESTAMP.eq(
                select(max(Measurement.TIMESTAMP))
                    .from(Measurement)
                    .where(Measurement.SENSOR_ID.eq(Sensor.ID))
            )
                .or(Measurement.ID.isNull)
        )
        .fetch()
    }
}


 */