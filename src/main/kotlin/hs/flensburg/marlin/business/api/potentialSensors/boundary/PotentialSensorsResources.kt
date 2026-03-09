package hs.flensburg.marlin.business.api.potentialSensors.boundary

import hs.flensburg.marlin.business.api.openAPI.PotentialSensorsOpenAPISpec
import hs.flensburg.marlin.business.schedulerJobs.potentialSensors.boundary.PotentialSensorService
import hs.flensburg.marlin.plugins.Realm
import hs.flensburg.marlin.plugins.authenticate
import hs.flensburg.marlin.plugins.respondKIO
import io.github.smiley4.ktoropenapi.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.routing.routing

fun Application.configurePotentialSensors() {
    routing {
        authenticate(Realm.ADMIN) {
            get("/admin/potential-sensors", PotentialSensorsOpenAPISpec.getPotentialSensors) {
                call.respondKIO(PotentialSensorService.getAllPotentialSensors())
            }

            get(
                "/admin/potential-sensors-toggle/{id}",
                PotentialSensorsOpenAPISpec.getPotentialSensorToggle
            ) {
                val id = call.parameters["id"]?.toLongOrNull()

                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid id")
                    return@get
                }

                call.respondKIO(PotentialSensorService.toggleIsActive(id))
            }
        }
    }
}