package hs.flensburg.marlin.business.api.openAPI

import hs.flensburg.marlin.business.api.location.entity.DetailedLocationDTO
import hs.flensburg.marlin.business.api.sensors.entity.LocationWithBoxesDTO
import hs.flensburg.marlin.business.api.sensors.entity.LocationWithLatestMeasurementsDTO
import hs.flensburg.marlin.business.api.sensors.entity.UnitsWithLocationWithBoxesDTO
import hs.flensburg.marlin.business.api.sensors.entity.raw.MeasurementDTO
import hs.flensburg.marlin.business.api.sensors.entity.raw.MeasurementTypeDTO
import hs.flensburg.marlin.business.api.sensors.entity.raw.SensorDTO
import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.ktor.http.HttpStatusCode

object SensorsOpenAPISpec {

    val getAllSensors: RouteConfig.() -> Unit = {
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

    val getAllMeasurementTypes: RouteConfig.() -> Unit = {
        tags("raw")
        description = "Return all measurement types (raw form)."
        response {
            HttpStatusCode.OK to {
                description = "List of measurement types"
                body<List<MeasurementTypeDTO>>()
            }
        }
    }

    val getAllLocations: RouteConfig.() -> Unit = {
        specName = "public"
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

    val getAllMeasurements: RouteConfig.() -> Unit = {
        specName = "public"
        tags("measurements")
        description = "Return all measurements (raw form)."
        response {
            HttpStatusCode.OK to {
                description = "List of measurements"
                body<List<MeasurementDTO>>()
            }
        }
    }

    val getLatestMeasurements: RouteConfig.() -> Unit = {
        tags("measurements")
        description = "Return the latest measurement for each location (raw form)."
        response {
            HttpStatusCode.OK to {
                description = "List of locations with their latest measurements"
                body<List<LocationWithLatestMeasurementsDTO>>()
            }
        }
    }

    val getLatestMeasurementsNew: RouteConfig.() -> Unit = {
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

    val getLocationMeasurementsWithinTimeRangeFast: RouteConfig.() -> Unit = {
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

    val getLatestMeasurementsV3: RouteConfig.() -> Unit = {
        specName = "public"
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

    val getLocationLatestMeasurements: RouteConfig.() -> Unit = {
        specName = "public"
        tags("location")
        description = "Get the latest measurements for a single location."
        request {
            pathParameter<Long>("id") {
                description = "The location ID (not the sensor ID)"
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
                description = "Successful response with latest measurements for a location"
                body<UnitsWithLocationWithBoxesDTO>()
            }
            HttpStatusCode.InternalServerError to {
                description = "Error occurred while retrieving the latest measurements"
            }
        }
    }

    val getLocationMeasurementsWithinTimeRangeV3: RouteConfig.() -> Unit = {
        specName = "public"
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
}
