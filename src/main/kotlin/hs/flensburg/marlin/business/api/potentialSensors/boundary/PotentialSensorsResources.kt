package hs.flensburg.marlin.business.api.potentialSensors.boundary

import de.lambda9.tailwind.core.KIO
import hs.flensburg.marlin.business.ApiError
import hs.flensburg.marlin.business.api.potentialSensors.entity.PotentialSensorDTO
import hs.flensburg.marlin.business.schedulerJobs.potentialSensors.boundary.PotentialSensorService
import hs.flensburg.marlin.database.generated.tables.pojos.PotentialSensor
import hs.flensburg.marlin.plugins.respondKIO
import io.github.smiley4.ktoropenapi.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.routing.routing

fun Application.configurePotentialSensors() {
    routing {
        // TODO: ADMIN Authentication
        get(
            path = "/potential-sensors",
            builder = {
                description = "Get all potential sensors"
                tags("admin", "potential-sensors")
                response {
                    HttpStatusCode.OK to {
                        description = "List of potential sensors"
                        body<List<PotentialSensorDTO>>()
                    }
                    HttpStatusCode.InternalServerError to {
                        description = "Error retrieving potential sensors"
                    }
                }
            }
        ) {
            call.respondKIO(PotentialSensorService.getAllPotentialSensors())
        }
        get(
            path = "/potential-sensors-toggle/{id}",
            builder = {
                description = "Toggle active state of potential sensors"
                tags("admin", "potential-sensors")
                request {
                    pathParameter<Long>("id") {
                        description = "The sensor ID"
                    }
                }
                response {
                    HttpStatusCode.OK to {
                        description = "potential sensors with updated active state"
                        body<List<PotentialSensorDTO>>()
                    }
                    HttpStatusCode.InternalServerError to {
                        description = "Error retrieving potential sensors"
                    }
                }
            }
        ) {
            val id = call.parameters["id"]?.toLongOrNull()
            println(id)

            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, "Invalid id")
                return@get
            }

            call.respondKIO(PotentialSensorService.toggleIsActive(id))
        }
    }
}