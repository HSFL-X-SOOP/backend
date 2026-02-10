package hs.flensburg.marlin.business.api.sensors.boundary

import hs.flensburg.marlin.business.api.auth.entity.LoggedInUser
import hs.flensburg.marlin.business.api.location.boundary.LocationService
import hs.flensburg.marlin.business.api.location.entity.DetailedLocationDTO
import hs.flensburg.marlin.business.api.sensors.entity.LocationWithBoxesDTO
import hs.flensburg.marlin.business.api.sensors.entity.LocationWithLatestMeasurementsDTO
import hs.flensburg.marlin.business.api.sensors.entity.SensorMeasurementsTimeRange
import hs.flensburg.marlin.business.api.sensors.entity.UnitsWithLocationWithBoxesDTO
import hs.flensburg.marlin.business.api.sensors.entity.raw.MeasurementDTO
import hs.flensburg.marlin.business.api.sensors.entity.raw.MeasurementTypeDTO
import hs.flensburg.marlin.business.api.sensors.entity.raw.SensorDTO
import hs.flensburg.marlin.business.api.timezones.boundary.TimezonesService
import hs.flensburg.marlin.plugins.Realm
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
                tags("location")
                description = "Return all locations."
                request {
                    queryParameter<String>("timezone") {
                        description =
                            "Optional timezone ('Europe/Berlin'). Defaults to Ip address based timezone. Backup UTC."
                        required = false
                    }
                }
                response {
                    HttpStatusCode.OK to {
                        description = "List of locations"
                        body<List<DetailedLocationDTO>>()
                    }
                }
            }
        ) {
            val result = TimezonesService.withResolvedTimezone<List<DetailedLocationDTO>>(
                call.parameters["timezone"],
                call.request.origin.remoteAddress
            ) { tz ->
                LocationService.getAllLocations(tz)
            }
            call.respondKIO(result)
        }

        get(
            path = "/measurements",
            builder = {
                tags("measurements")
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
                tags("measurements")
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
                description =
                    "Get the latest measurement values for all locations. The measurement must be within the last 2 hours."
                request {
                    queryParameter<String>("timezone") {
                        description =
                            "Optional timezone ('Europe/Berlin'). Defaults to Ip address based timezone. Backup UTC."
                        required = false
                    }
                    queryParameter<String>("units") {
                        description =
                            "Optional units for the measurements ('metric, imperial, custom'). Defaults to metric."
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
                SensorService.getLocationsWithLatestMeasurementsNEW(
                    call.parameters["timezone"] ?: "DEFAULT",
                    call.request.origin.remoteAddress,
                    call.parameters["units"] ?: "metric"
                )
            )
        }

        get(
            path = "/location/{id}/measurementsWithinTimeRangeFAST",
            builder = {
                tags("location")
                description = "Get all measurements for a location within a given time range"
                request {
                    pathParameter<Long>("id") {
                        description = "The location ID (not the sensor ID)"
                    }
                    queryParameter<String>("timeRange") {
                        description = """Optional time range ('48h', '7d', '30d', '1y'). Defaults to 24h.
                            |           "24h" -> raw;
                                        "48h" -> raw;
                                        "7d"  -> avg: 2 hours;
                                        "30d" -> avg: 6 hours;
                                        "90d"  -> avg: 12 hours;
                                        "180d" -> avg: 1 day;
                                        "1y"  -> avg: 2 days;
                        """.trimMargin()
                        required = false
                    }
                    queryParameter<String>("timezone") {
                        description =
                            "Optional timezone ('Europe/Berlin'). Defaults to Ip address based timezone. Backup UTC."
                        required = false
                    }
                    queryParameter<String>("units") {
                        description =
                            "Optional units for the measurements ('metric, imperial, custom'). Defaults to metric."
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
            get(
                path = "/latestmeasurements_v3",
                builder = {
                    tags("measurements")
                    description =
                        "Get the latest measurement values for all locations. The measurement must be within the last 2 hours. Version 3."
                    request {
                        queryParameter<String>("timezone") {
                            description =
                                "Optional timezone ('Europe/Berlin'). Defaults to Ip address based timezone. Backup UTC."
                            required = false
                        }
                        queryParameter<String>("units") {
                            description =
                                "Optional units for the measurements ('metric, imperial, custom'). Defaults to metric."
                            required = false
                        }
                    }
                    response {
                        HttpStatusCode.OK to {
                            description = "Successful response with latest measurements for each location"
                            body<UnitsWithLocationWithBoxesDTO>()
                        }
                        HttpStatusCode.InternalServerError to {
                            description = "Error occurred while retrieving the latest measurements"
                        }
                    }
                }
            ) {
                val user = call.principal<LoggedInUser>()
                call.respondKIO(
                    SensorService.getLocationsWithLatestMeasurementsV3(
                        call.parameters["timezone"] ?: "DEFAULT",
                        call.request.origin.remoteAddress,
                        call.parameters["units"],
                        user?.id
                    )
                )
            }

            get(
                path = "/location/{id}/measurementsWithinTimeRange_v3",
                builder = {
                    tags("location")
                    description = "Get all measurements for a location within a given time range"
                    request {
                        pathParameter<Long>("id") {
                            description = "The location ID (not the sensor ID)"
                        }
                        queryParameter<String>("timeRange") {
                            description = """Optional time range ('48h', '7d', '30d', '1y'). Defaults to 24h.
                            |           "24h" -> raw;
                                        "48h" -> raw;
                                        "7d"  -> avg: 2 hours;
                                        "30d" -> avg: 6 hours;
                                        "90d"  -> avg: 12 hours;
                                        "180d" -> avg: 1 day;
                                        "1y"  -> avg: 2 days;
                        """.trimMargin()
                            required = false
                        }
                        queryParameter<String>("timezone") {
                            description =
                                "Optional timezone ('Europe/Berlin'). Defaults to Ip address based timezone. Backup UTC."
                            required = false
                        }
                        queryParameter<String>("units") {
                            description =
                                "Optional units for the measurements ('metric, imperial, custom'). Defaults to metric."
                            required = false
                        }
                    }
                    response {
                        HttpStatusCode.OK to {
                            description = "Successful response with measurements"
                            body<UnitsWithLocationWithBoxesDTO>()
                        }
                        HttpStatusCode.BadRequest to {
                            description = "Invalid parameters"
                        }
                    }
                }
            ) {
                val locationId = call.parameters["id"]?.toLongOrNull()
                    ?: return@get call.respondText("Missing or wrong id", status = HttpStatusCode.BadRequest)

                val timeRange = call.parameters["timeRange"] ?: "24h"

                val user = call.principal<LoggedInUser>()

                call.respondKIO(
                    SensorService.getLocationByIDWithMeasurementsWithinTimespanV3(
                        locationId,
                        SensorMeasurementsTimeRange.fromString(timeRange) ?: return@get call.respondText(
                            "wrong timeRange",
                            status = HttpStatusCode.BadRequest
                        ),
                        call.parameters["timezone"] ?: "DEFAULT",
                        call.request.origin.remoteAddress,
                        call.parameters["units"],
                        user?.id
                    )
                )
            }
        }

    }
}