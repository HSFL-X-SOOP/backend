package hs.flensburg.soop.business.api

import de.lambda9.tailwind.core.KIO.Companion.unsafeRunSync
import de.lambda9.tailwind.jooq.Jooq
import hs.flensburg.soop.business.App
import hs.flensburg.soop.business.JEnv
import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import org.jooq.exception.DataAccessException
import hs.flensburg.soop.database.generated.tables.pojos.Sensor
import hs.flensburg.soop.database.generated.tables.references.SENSOR

/*
fun Application.registerLocationRoutes(env: JEnv) {
    routing {
        get("/locations") {
            getSensorDataFromDB().unsafeRunSync(env)
        }
    }
}


 */
fun getAllSensorsFromDB(): App<DataAccessException, List<Sensor>> = Jooq.query {
    selectFrom(SENSOR).fetchInto(Sensor::class.java)
}
/*
fun getSensorDataFromDB(): App<DataAccessException, Unit> = Jooq.query {
    val sensor = select(SENSOR)

    val result = select(
            LOCATION.ID,
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
                DSL.select(DSL.max(Measurement.TIMESTAMP))
                    .from(Measurement)
                    .where(Measurement.SENSOR_ID.eq(Sensor.ID))
            )
                .or(Measurement.ID.isNull)
        )
        .fetchMaps()

}
*/
