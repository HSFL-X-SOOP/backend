package hs.flensburg.marlin

import de.lambda9.tailwind.core.Exit.Companion.isSuccess
import de.lambda9.tailwind.core.KIO
import de.lambda9.tailwind.core.KIO.Companion.unsafeRunSync
import de.lambda9.tailwind.core.extensions.exit.getOrNull
import hs.flensburg.marlin.Config.Companion.parseConfig
import hs.flensburg.marlin.business.Env
import hs.flensburg.marlin.business.JEnv
import hs.flensburg.marlin.business.api.sensors.boundary.SensorQueryService
import hs.flensburg.marlin.business.api.sensors.entity.raw.LocationDTO
import hs.flensburg.marlin.business.api.sensors.entity.LocationWithBoxesDTO
import hs.flensburg.marlin.business.api.sensors.entity.LocationWithLatestMeasurementsDTO
import hs.flensburg.marlin.business.api.sensors.entity.raw.MeasurementDTO
import hs.flensburg.marlin.business.api.sensors.entity.raw.MeasurementTypeDTO
import hs.flensburg.marlin.business.api.sensors.entity.raw.SensorDTO
import hs.flensburg.marlin.business.api.sensors.entity.boxes.mapSensorToBoxDTO
import hs.flensburg.marlin.business.api.sensors.entity.raw.toLocationDTO
import hs.flensburg.marlin.business.api.sensors.entity.raw.toMeasurementDTO
import hs.flensburg.marlin.business.api.sensors.entity.raw.toMeasurementTypeDTO
import hs.flensburg.marlin.business.api.sensors.entity.raw.toSensorDTO
import hs.flensburg.marlin.business.api.timezones.boundary.TimezonesService
import hs.flensburg.marlin.database.generated.tables.pojos.Location
import hs.flensburg.marlin.database.generated.tables.pojos.Measurement
import hs.flensburg.marlin.database.generated.tables.pojos.Measurementtype
import hs.flensburg.marlin.database.generated.tables.pojos.Sensor
import hs.flensburg.marlin.business.configureScheduling
import hs.flensburg.marlin.plugins.configureCORS
import hs.flensburg.marlin.plugins.configureKIO
import hs.flensburg.marlin.plugins.configureRouting
import hs.flensburg.marlin.plugins.configureSerialization
import hs.flensburg.marlin.plugins.respondKIO
import io.github.cdimascio.dotenv.dotenv
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.smiley4.ktoropenapi.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import org.flywaydb.core.Flyway
import io.ktor.server.response.respond
import io.ktor.server.routing.routing

private val logger = KotlinLogging.logger { }

fun main(args: Array<String>) {
    val mode = System.getenv("MODE")?.uppercase() ?: "DEV"

    logger.info { "Starting Marlin-Backend in $mode mode" }

    val config = when (mode) {
        "DEV" -> dotenv()
        else -> null
    }

    val (env, dsl) = Env.configure(config?.parseConfig() ?: Config.parseConfig())

    Flyway(
        Flyway.configure()
            .driver("org.postgresql.Driver")
            .dataSource(dsl)
            .schemas("marlin")
    ).migrate()

    embeddedServer(
        factory = Netty,
        port = env.env.config.http.port,
        host = env.env.config.http.host,
        module = { modules(env) }
    ).start(wait = true)
}

fun Application.modules(env: JEnv) {
    configureSerialization()
    configureKIO(env)
    configureScheduling(env)
    configureCORS()
    configureRouting(env.env.config)

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
            val response = SensorQueryService.getAllSensorsFromDB().unsafeRunSync(env)
            if (response.isSuccess()) {
                val sensors: List<Sensor> = response.getOrNull()!!
                val sensorDTOs: List<SensorDTO> = sensors.map { it.toSensorDTO() }
                call.respond(sensorDTOs)
            } else {
                call.respondKIO(KIO.ok("Fehler beim Abrufen der Sensoren $response"))
            }
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
            val response = SensorQueryService.getAllMeasurementTypesFromDB().unsafeRunSync(env)
            if (response.isSuccess()) {
                val measurementtypes: List<Measurementtype> = response.getOrNull()!!
                val measurementtypeDTOs: List<MeasurementTypeDTO> = measurementtypes.map { it.toMeasurementTypeDTO() }
                call.respond(measurementtypeDTOs)
            } else {
                call.respondKIO(KIO.ok("Fehler beim Abrufen der Sensoren $response"))
            }
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
            val response = SensorQueryService.getAllLocationsFromDB().unsafeRunSync(env)
            if (response.isSuccess()) {
                val locations: List<Location> = response.getOrNull()!!
                val locationDTOs: List<LocationDTO> = locations.map { it.toLocationDTO() }
                call.respond(locationDTOs)
            } else {
                call.respondKIO(KIO.ok("Fehler beim Abrufen der Sensoren $response"))
            }
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
            val response = SensorQueryService.getAllMeasurementsFromDB().unsafeRunSync(env)
            if (response.isSuccess()) {
                val measurements: List<Measurement> = response.getOrNull()!!
                val measurementDTOs: List<MeasurementDTO> = measurements.map { it.toMeasurementDTO() }
                call.respond(measurementDTOs)
            } else {
                call.respondKIO(KIO.ok("Fehler beim Abrufen der Sensoren $response"))
            }
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
            val response = SensorQueryService.getLocationsWithLatestMeasurements("").unsafeRunSync(env)
            if (response.isSuccess()) {
                val result = response.getOrNull()!!
                call.respond(result)
            } else {
                call.respondKIO(KIO.ok("Fehler beim Abrufen der Messdaten $response"))
            }
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
            val timezone = TimezonesService.getClientTimeZoneFromIPOrQueryParam(call)

            val response = SensorQueryService.getLocationsWithLatestMeasurements(timezone).unsafeRunSync(env)

            if (response.isSuccess()) {
                val rawLocations = response.getOrNull()!!

                val result = rawLocations.map { loc ->
                    val boxes = loc.latestMeasurements
                        .groupBy { it.sensor.id } // collect all measurements of the same sensor
                        .map { (_, measurements) ->
                            val sensor = measurements.first().sensor
                            mapSensorToBoxDTO(sensor, measurements)
                        }

                    LocationWithBoxesDTO(
                        location = loc.location,
                        boxes = boxes
                    )
                }

                call.respond(result)
            } else {
                call.respondKIO(KIO.ok("Fehler beim Abrufen der Messdaten $response"))
            }
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
                        description = "Optional time range ('today', 'week', 'month', 'DEFAULT'). Defaults to 24h."
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

            val timezone = TimezonesService.getClientTimeZoneFromIPOrQueryParam(call)

            val response = SensorQueryService.getLocationByIDWithMeasurementsWithinTimespan(locationID, timeRange, timezone).unsafeRunSync(env)

            if (response.isSuccess()) {
                val rawLocation = response.getOrNull()!!

                val boxes = rawLocation.latestMeasurements
                    .groupBy { it.sensor.id } // one box per sensor
                    .map { (_, measurements) ->
                        val sensor = measurements.first().sensor
                        mapSensorToBoxDTO(sensor, measurements) // this function already groups by time
                    }

                val result = LocationWithBoxesDTO(
                    location = rawLocation.location,
                    boxes = boxes
                )

                call.respond(result)
            } else {
                call.respondKIO(KIO.ok("Fehler beim Abrufen der Messdaten $response"))
            }
        }
    }
}
