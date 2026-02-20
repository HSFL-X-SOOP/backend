package hs.flensburg.marlin.business.api.sensors.boundary

import de.lambda9.tailwind.core.KIO.Companion.unsafeRunSync
import de.lambda9.tailwind.core.extensions.exit.getOrElse
import de.lambda9.tailwind.core.extensions.exit.getOrNull
import hs.flensburg.marlin.business.api.auth.entity.LoggedInUser
import hs.flensburg.marlin.business.api.location.boundary.LocationService
import hs.flensburg.marlin.business.api.sensors.entity.SensorMeasurementsTimeRange
import hs.flensburg.marlin.business.api.timezones.boundary.TimezonesService
import hs.flensburg.marlin.business.api.units.boundary.UnitsService
import hs.flensburg.marlin.business.api.openAPI.SensorsOpenAPISpec
import hs.flensburg.marlin.plugins.Realm
import hs.flensburg.marlin.plugins.kioEnv
import hs.flensburg.marlin.plugins.respondKIO
import io.github.smiley4.ktoropenapi.get
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureSensors() {
    routing {
        get("/sensors", SensorsOpenAPISpec.getAllSensors) {
            call.respondKIO(SensorService.getAllSensors())
        }

        get("/measurementtypes", SensorsOpenAPISpec.getAllMeasurementTypes) {
            call.respondKIO(SensorService.getAllMeasurementTypes())
        }

        get("/locations", SensorsOpenAPISpec.getAllLocations) {
            val tz = TimezonesService.getClientTimeZoneFromIPOrQueryParam(
                call.parameters["timezone"],
                call.request.origin.remoteAddress
            )

            call.respondKIO(LocationService.getAllLocations(tz))
        }

        get("/measurements", SensorsOpenAPISpec.getAllMeasurements) {
            call.respondKIO(SensorService.getAllMeasurements())
        }

        get("/latestmeasurements", SensorsOpenAPISpec.getLatestMeasurements) {
            call.respondKIO(SensorService.getLocationsWithLatestMeasurements(""))
        }

        get("/latestmeasurementsNEW", SensorsOpenAPISpec.getLatestMeasurementsNew) {
            call.respondKIO(
                SensorService.getLocationsWithLatestMeasurementsNEW(
                    call.parameters["timezone"] ?: "DEFAULT",
                    call.request.origin.remoteAddress,
                    call.parameters["units"] ?: "metric"
                )
            )
        }

        get("/location/{id}/measurementsWithinTimeRangeFAST", SensorsOpenAPISpec.getLocationMeasurementsWithinTimeRangeFast) {
            val locationId = call.parameters["id"]?.toLongOrNull()
                ?: return@get call.respondText("Missing or wrong id", status = HttpStatusCode.BadRequest)

            val timeRange = call.parameters["timeRange"] ?: "24h"

            call.respondKIO(
                SensorService.getLocationByIDWithMeasurementsWithinTimespanFAST(
                    locationId,
                    SensorMeasurementsTimeRange.fromString(timeRange) ?: return@get call.respondText(
                        "wrong timeRange",
                        status = HttpStatusCode.BadRequest
                    ),
                    call.parameters["timezone"] ?: "DEFAULT",
                    call.request.origin.remoteAddress,
                    call.parameters["units"] ?: "metric"
                )
            )
        }
        authenticate(Realm.COMMON.toString(), optional = true) {
            get("/latestmeasurements_v3", SensorsOpenAPISpec.getLatestMeasurementsV3) {
                val user = call.principal<LoggedInUser>()
                val units =
                    UnitsService.withResolvedUnits(call.parameters["units"], user?.id).unsafeRunSync(call.kioEnv)
                        .getOrElse {
                            return@get call.respond(HttpStatusCode.InternalServerError)
                        }

                call.respondKIO(
                    SensorService.getLocationsWithLatestMeasurementsV3(
                        call.parameters["timezone"] ?: "DEFAULT",
                        call.request.origin.remoteAddress,
                        units,
                    )
                )
            }

            get("/location/{id}/latestmeasurements", SensorsOpenAPISpec.getLocationLatestMeasurements) {
                val locationId = call.parameters["id"]?.toLongOrNull()
                    ?: return@get call.respondText("Missing or wrong id", status = HttpStatusCode.BadRequest)

                val user = call.principal<LoggedInUser>()

                val tz = TimezonesService.getClientTimeZoneFromIPOrQueryParam(
                    call.parameters["timezone"],
                    call.request.origin.remoteAddress
                )

                val units =
                    UnitsService.withResolvedUnits(call.parameters["units"], user?.id)
                        .unsafeRunSync(call.kioEnv)
                        .getOrElse {
                            return@get call.respond(HttpStatusCode.InternalServerError)
                        }

                call.respondKIO(SensorService.getSingleLocationWithLatestMeasurements(locationId, tz, units))
            }

            get("/location/{id}/measurementsWithinTimeRange_v3", SensorsOpenAPISpec.getLocationMeasurementsWithinTimeRangeV3) {
                val locationId = call.parameters["id"]?.toLongOrNull()
                    ?: return@get call.respondText("Missing or wrong id", status = HttpStatusCode.BadRequest)

                val user = call.principal<LoggedInUser>()

                val timeRange = SensorMeasurementsTimeRange.fromString(call.parameters["timeRange"] ?: "24h")
                    ?: return@get call.respondText(
                        "wrong timeRange",
                        status = HttpStatusCode.BadRequest
                    )

                val units =
                    UnitsService.withResolvedUnits(call.parameters["units"], user?.id).unsafeRunSync(call.kioEnv)
                        .getOrElse {
                            return@get call.respond(HttpStatusCode.InternalServerError)
                        }



                call.respondKIO(
                    SensorService.getLocationByIDWithMeasurementsWithinTimespanV3(
                        locationId,
                        timeRange,
                        call.parameters["timezone"] ?: "DEFAULT",
                        call.request.origin.remoteAddress,
                        units,
                    )
                )
            }
        }
    }
}
