package hs.flensburg.marlin

import de.lambda9.tailwind.core.Exit.Companion.isSuccess
import de.lambda9.tailwind.core.KIO
import de.lambda9.tailwind.core.KIO.Companion.unsafeRunSync
import de.lambda9.tailwind.core.extensions.exit.getOrNull
import hs.flensburg.marlin.Config.Companion.parseConfig
import hs.flensburg.marlin.business.Env
import hs.flensburg.marlin.business.JEnv
import hs.flensburg.marlin.business.api.dto.LocationDTO
import hs.flensburg.marlin.business.api.dto.MeasurementDTO
import hs.flensburg.marlin.business.api.dto.MeasurementTypeDTO
import hs.flensburg.marlin.business.api.dto.SensorDTO
import hs.flensburg.marlin.business.api.dto.toLocationDTO
import hs.flensburg.marlin.business.api.dto.toMeasurementDTO
import hs.flensburg.marlin.business.api.dto.toMeasurementTypeDTO
import hs.flensburg.marlin.business.api.dto.toSensorDTO
import hs.flensburg.marlin.business.api.getAllLocationsFromDB
import hs.flensburg.marlin.business.api.getAllMeasurementTypesFromDB
import hs.flensburg.marlin.business.api.getAllMeasurementsFromDB
import hs.flensburg.marlin.business.api.getAllSensorsFromDB
import hs.flensburg.marlin.business.api.getLocationsWithLatestMeasurements
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
import io.ktor.server.application.Application
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import org.flywaydb.core.Flyway
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.route
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
        route("/sensors") {
            get {
                // TODO: Wrap in KIO
                // response: Exit<DataException, List<Sensor>>
                val response = getAllSensorsFromDB().unsafeRunSync(env)
                if (response.isSuccess()) {
                    //sensors: List<Sensor>
                    val sensors: List<Sensor> = response.getOrNull()!!
                    val sensorDTOs: List<SensorDTO> = sensors.map { it.toSensorDTO() }
                    call.respond(sensorDTOs)
                }else{
                    call.respondKIO(KIO.ok("Fehler beim Abrufen der Sensoren ${response}"))
                }
            }
        }
        route("/measurementtypes") {
            get {
                // TODO: Wrap in KIO
                val response = getAllMeasurementTypesFromDB().unsafeRunSync(env)
                if (response.isSuccess()) {
                    val measurementtypes: List<Measurementtype> = response.getOrNull()!!
                    val measurementtypeDTOs: List<MeasurementTypeDTO> = measurementtypes.map { it.toMeasurementTypeDTO() }
                    call.respond(measurementtypeDTOs)
                }else{
                    call.respondKIO(KIO.ok("Fehler beim Abrufen der Sensoren ${response}"))
                }
            }
        }
        route("/locations") {
            get {
                // TODO: Wrap in KIO
                val response = getAllLocationsFromDB().unsafeRunSync(env)
                if (response.isSuccess()) {
                    val locations: List<Location> = response.getOrNull()!!
                    val locationDTOs: List<LocationDTO> = locations.map { it.toLocationDTO() }
                    call.respond(locationDTOs)
                }else{
                    call.respondKIO(KIO.ok("Fehler beim Abrufen der Sensoren ${response}"))
                }
            }
        }
        route("/measurements") {
            get {
                // TODO: Wrap in KIO
                val response = getAllMeasurementsFromDB().unsafeRunSync(env)
                if (response.isSuccess()) {
                    val measurements: List<Measurement> = response.getOrNull()!!
                    val measurementDTOs: List<MeasurementDTO> = measurements.map { it.toMeasurementDTO() }
                    call.respond(measurementDTOs)
                }else{
                    call.respondKIO(KIO.ok("Fehler beim Abrufen der Sensoren ${response}"))
                }
            }
        }
        route("/latestmeasurements") {
            get {
                val response = getLocationsWithLatestMeasurements().unsafeRunSync(env)
                if (response.isSuccess()) {
                    val result = response.getOrNull()!! // List<LocationWithLatestMeasurementsDTO>
                    call.respond(result)
                } else {
                    call.respondKIO(KIO.ok("Fehler beim Abrufen der Messdaten ${response}"))
                }
            }
        }

    }
}
