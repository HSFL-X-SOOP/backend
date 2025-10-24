package hs.flensburg.marlin.business.api.location.boundary

import de.lambda9.tailwind.core.KIO
import hs.flensburg.marlin.business.api.location.entity.DetailedLocationDTO
import hs.flensburg.marlin.business.api.location.entity.UpdateLocationRequest
import hs.flensburg.marlin.plugins.respondKIO
import io.github.smiley4.ktoropenapi.get
import io.github.smiley4.ktoropenapi.put
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.request.receive
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

        put(
            path = "/location/{id}",
            builder = {
                description = "Update the location information"
                tags("location")
                request {
                    pathParameter<Long>("id") {
                        description = "The location ID (not the sensor ID)"
                    }
                    body<UpdateLocationRequest>()
                }
                response {
                    HttpStatusCode.OK to {
                        body<DetailedLocationDTO>()
                    }
                    HttpStatusCode.NotFound to {
                        body<String>()
                    }
                }
            }
        ) {
            val locationID = call.parameters["id"]?.toLongOrNull()

            if (locationID == null) {
                call.respondKIO(KIO.ok("LocationID fehlt oder ungültig"))
                return@put
            }

            val request = call.receive<UpdateLocationRequest>()

            call.respondKIO(LocationService.updateLocationByID(locationID, request))
        }
    }
}