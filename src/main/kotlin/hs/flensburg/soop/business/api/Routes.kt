package hs.flensburg.soop.business.api

import de.lambda9.tailwind.core.KIO.Companion.unsafeRunSync
import de.lambda9.tailwind.jooq.Jooq
import hs.flensburg.soop.business.App
import hs.flensburg.soop.business.JEnv
import hs.flensburg.soop.database.generated.tables.pojos.Measurement
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
import hs.flensburg.soop.database.generated.tables.references.MEASUREMENT
import kotlin.text.get
import org.jooq.impl.DSL


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
