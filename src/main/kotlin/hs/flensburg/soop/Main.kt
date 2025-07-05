package hs.flensburg.soop

import de.lambda9.tailwind.core.Exit.Companion.isSuccess
import de.lambda9.tailwind.core.KIO
import de.lambda9.tailwind.core.KIO.Companion.unsafeRunSync
import de.lambda9.tailwind.core.extensions.exit.getOrNull
import hs.flensburg.soop.Config.Companion.parseConfig
import hs.flensburg.soop.business.Env
import hs.flensburg.soop.business.JEnv
import hs.flensburg.soop.business.api.dto.LocationDTO
import hs.flensburg.soop.business.api.dto.MeasurementTypeDTO
import hs.flensburg.soop.business.api.dto.SensorDTO
import hs.flensburg.soop.business.api.dto.toLocationDTO
import hs.flensburg.soop.business.api.dto.toMeasurementTypeDTO
import hs.flensburg.soop.business.api.dto.toSensorDTO
import hs.flensburg.soop.business.api.getAllLocationsFromDB
import hs.flensburg.soop.business.api.getAllMeasurementTypesFromDB
import hs.flensburg.soop.business.api.getAllSensorsFromDB
import hs.flensburg.soop.database.generated.tables.pojos.Location
import hs.flensburg.soop.database.generated.tables.pojos.Measurementtype
import hs.flensburg.soop.database.generated.tables.pojos.Sensor
import hs.flensburg.soop.plugins.configureKIO
import hs.flensburg.soop.plugins.configureSerialization
import hs.flensburg.soop.plugins.respondKIO
import io.github.cdimascio.dotenv.dotenv
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.application.Application
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

private val logger = KotlinLogging.logger { }

fun main(args: Array<String>) {
    val mode = System.getenv("MODE")?.uppercase() ?: "DEV"

    val config = when (mode) {
        "STAGING", "DEV" -> dotenv()
        else -> null
    }

    val (env, _) = Env.configure(config?.parseConfig() ?: Config.parseConfig())

    embeddedServer(
        factory = Netty,
        port = 8080,
        host = "0.0.0.0",
        module = { modules(env) }
    ).start(wait = true)
}

fun Application.modules(env: JEnv) {
    configureSerialization()
    configureKIO(env)

    routing {
        route("/test") {
            get {
                call.respondKIO(KIO.ok("This is a test response"))
            }
        }
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
                    call.respondKIO(KIO.ok("Fehler beim Abrufen der Sensoren"))
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
                    call.respondKIO(KIO.ok("Fehler beim Abrufen der Sensoren"))
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
    }
}
