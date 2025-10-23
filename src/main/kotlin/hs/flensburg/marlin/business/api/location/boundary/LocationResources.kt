package hs.flensburg.marlin.business.api.location.boundary

import de.lambda9.tailwind.core.KIO
import hs.flensburg.marlin.business.api.location.entity.DetailedLocationDTO
import hs.flensburg.marlin.business.api.sensors.boundary.SensorService
import hs.flensburg.marlin.business.api.sensors.entity.LocationWithBoxesDTO
import hs.flensburg.marlin.business.api.sensors.entity.raw.LocationDTO
import hs.flensburg.marlin.business.api.sensors.entity.raw.SensorDTO
import hs.flensburg.marlin.plugins.respondKIO
import io.github.smiley4.ktoropenapi.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.routing.routing

fun Application.configureLocation() {
    routing {
        get(
            path = "/location/{id}",
            builder = {
                tags("location")
                description = "Get a location"
                request {
                    pathParameter<Long>("id") {
                        description = "The location ID (not the sensor ID)"
                    }
                }
                response {
                    HttpStatusCode.OK to {
                        description = "Successful response with measurements"
                        body<DetailedLocationDTO>()
                    }
                    HttpStatusCode.BadRequest to {
                        description = "Invalid parameters"
                    }
                }
            }
        ) {
            val locationID = call.parameters["id"]?.toLongOrNull()

            if (locationID == null) {
                call.respondKIO(KIO.ok("LocationID fehlt oder ungültig"))
                return@get
            }

            call.respondKIO(LocationService.getLocationByID(locationID))
        }
    }
}