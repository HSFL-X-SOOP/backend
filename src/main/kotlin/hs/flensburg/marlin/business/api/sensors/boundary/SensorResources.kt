package hs.flensburg.marlin.business.api.sensors.boundary

import de.lambda9.tailwind.core.Exit.Companion.isSuccess
import de.lambda9.tailwind.core.KIO
import de.lambda9.tailwind.core.KIO.Companion.unsafeRunSync
import de.lambda9.tailwind.core.extensions.exit.getOrNull
import hs.flensburg.marlin.business.api.sensors.entity.LocationWithBoxesDTO
import hs.flensburg.marlin.business.api.sensors.entity.LocationWithLatestMeasurementsDTO
import hs.flensburg.marlin.business.api.sensors.entity.raw.LocationDTO
import hs.flensburg.marlin.business.api.sensors.entity.raw.MeasurementDTO
import hs.flensburg.marlin.business.api.sensors.entity.raw.MeasurementTypeDTO
import hs.flensburg.marlin.business.api.sensors.entity.raw.SensorDTO
import hs.flensburg.marlin.business.api.sensors.entity.raw.toSensorDTO
import hs.flensburg.marlin.business.api.timezones.boundary.TimezonesService
import hs.flensburg.marlin.database.generated.tables.pojos.Sensor
import hs.flensburg.marlin.plugins.respondKIO
import io.github.smiley4.ktoropenapi.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.plugins.origin
import io.ktor.server.response.respond
import io.ktor.server.routing.routing
import kotlin.text.get

fun Application.configureSensors() {
    routing {
        get(
            path = "/sensors",
            builder = {
                tags("raw")
                description = "Return all sensors from the database (raw form, no aggregation)."
                response {
                    HttpStatusCode.OK to {
                        description = "List of sensors"
                        body<List<SensorDTO>>()
                    }
                    HttpStatusCode.InternalServerError to {
                        description = "Error retrieving sensors"
                    }
                }
            }
        ) {
            call.respondKIO(SensorService.getAllSensors())
        }

        get(
            path = "/measurementtypes",
            builder = {
                tags("raw")
                description = "Return all measurement types (raw form)."
                response {
                    HttpStatusCode.OK to {
                        description = "List of measurement types"
                        body<List<MeasurementTypeDTO>>()
                    }
                }
            }
        ) {
            call.respondKIO(SensorService.getAllMeasurementTypes())
        }

        get(
            path = "/locations",
            builder = {
                tags("raw")
                description = "Return all locations (raw form)."
                response {
                    HttpStatusCode.OK to {
                        description = "List of locations"
                        body<List<LocationDTO>>()
                    }
                }
            }
        ) {
            call.respondKIO(SensorService.getAllLocations())
        }

        get(
            path = "/measurements",
            builder = {
                tags("raw")
                description = "Return all measurements (raw form)."
                response {
                    HttpStatusCode.OK to {
                        description = "List of measurements"
                        body<List<MeasurementDTO>>()
                    }
                }
            }
        ) {
            call.respondKIO(SensorService.getAllMeasurements())
        }

        get(
            path = "/latestmeasurements",
            builder = {
                tags("raw")
                description = "Return the latest measurement for each location (raw form)."
                response {
                    HttpStatusCode.OK to {
                        description = "List of locations with their latest measurements"
                        body<List<LocationWithLatestMeasurementsDTO>>()
                    }
                }
            }
        ) {
            call.respondKIO(SensorService.getLocationsWithLatestMeasurements(""))
        }

        get(
            path = "/latestmeasurementsNEW",
            builder = {
                tags("measurements")
                description = "Get the latest measurement values for all locations. The measurement must be within the last 2 hours."
                request {
                    queryParameter<String>("timezone") {
                        description = "Optional timezone ('Europe/Berlin'). Defaults to Ip address based timezone. Backup UTC."
                        required = false
                    }
                }
                response {
                    HttpStatusCode.OK to {
                        description = "Successful response with latest measurements for each location"
                        body<List<LocationWithBoxesDTO>>()
                    }
                    HttpStatusCode.InternalServerError to {
                        description = "Error occurred while retrieving the latest measurements"
                    }
                }
            }
        ) {
            call.respondKIO(
                SensorService.getLocationWithLatestMeasurementsNEW(
                    call.parameters["timezone"] ?: "DEFAULT",
                    call.request.origin.remoteAddress
                )
            )
        }

        get(
            path = "/location/{id}/measurementsWithinTimeRange",
            builder = {
                tags("location")
                description = "Get all measurements for a location within a given time range"
                request {
                    pathParameter<Long>("id") {
                        description = "The location ID (not the sensor ID)"
                    }
                    queryParameter<String>("timeRange") {
                        description = "Optional time range ('48h', '7d', '30d'). Defaults to 24h."
                        required = false
                    }
                    queryParameter<String>("timezone") {
                        description = "Optional timezone ('Europe/Berlin'). Defaults to Ip address based timezone. Backup UTC."
                        required = false
                    }
                }
                response {
                    HttpStatusCode.OK to {
                        description = "Successful response with measurements"
                        body<LocationWithBoxesDTO>()
                    }
                    HttpStatusCode.BadRequest to {
                        description = "Invalid parameters"
                    }
                }
            }
        ) {
            val locationID = call.parameters["id"]?.toLongOrNull()
            val timeRange = call.parameters["timeRange"] ?: "DEFAULT"

            if (locationID == null) {
                call.respondKIO(KIO.ok("LocationID fehlt oder ungültig"))
                return@get
            }
            call.respondKIO(
                SensorService.getLocationByIDWithMeasurementsWithinTimespan(
                    locationID,
                    timeRange,
                    call.parameters["timezone"] ?: "DEFAULT",
                    call.request.origin.remoteAddress
                )
            )
        }
    }
}