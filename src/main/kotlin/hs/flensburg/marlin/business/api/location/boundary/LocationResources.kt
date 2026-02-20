package hs.flensburg.marlin.business.api.location.boundary

import de.lambda9.tailwind.core.KIO.Companion.unsafeRunSync
import hs.flensburg.marlin.business.api.auth.entity.LoggedInUser
import hs.flensburg.marlin.business.api.location.entity.Contact
import hs.flensburg.marlin.business.api.location.entity.DetailedLocationDTO
import hs.flensburg.marlin.business.api.location.entity.ImageRequest
import hs.flensburg.marlin.business.api.location.entity.UpdateLocationRequest
import hs.flensburg.marlin.business.api.timezones.boundary.TimezonesService
import hs.flensburg.marlin.plugins.Realm
import hs.flensburg.marlin.plugins.authenticate
import hs.flensburg.marlin.plugins.kioEnv
import hs.flensburg.marlin.plugins.respondKIO
import io.github.smiley4.ktoropenapi.delete
import io.github.smiley4.ktoropenapi.get
import io.github.smiley4.ktoropenapi.put
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.server.application.Application
import io.ktor.server.auth.principal
import io.ktor.server.plugins.origin
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondFile
import io.ktor.server.response.respondText
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
                    queryParameter<String>("timezone") {
                        description =
                            "Optional timezone ('Europe/Berlin'). Defaults to Ip address based timezone. Backup UTC."
                        required = false
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
            val locationId = call.parameters["id"]?.toLongOrNull()
                ?: return@get call.respondText("Missing or wrong id", status = HttpStatusCode.BadRequest)

            val tz = TimezonesService.getClientTimeZoneFromIPOrQueryParam(
                call.parameters["timezone"],
                call.request.origin.remoteAddress
            )

            call.respondKIO(LocationService.getLocationByID(locationId, tz))
        }

        get(
            path = "/location/{id}/image",
            builder = {
                tags("location")
                description = "Get a location image"
                request {
                    pathParameter<Long>("id") {
                        description = "The location ID (not the sensor ID)"
                    }
                }
                response {
                    HttpStatusCode.OK to {
                        description = "Successful response with image"
                        body<PartData.FileItem>()
                    }
                    HttpStatusCode.BadRequest to {
                        description = "Invalid parameters"
                    }
                }
            }
        ) {
            val id = call.parameters["id"]?.toLongOrNull()
                ?: return@get call.respondText("Missing or wrong id", status = HttpStatusCode.BadRequest)

            val image = LocationService.getLocationImage(id).unsafeRunSync(call.kioEnv)
                .fold(
                    onSuccess = { it },
                    onError = { error ->
                        val e = error.failures().firstOrNull()?.toApiError()
                        if (e != null) {
                            call.respond(e.statusCode, e.message)
                        } else {
                            call.respond(HttpStatusCode.InternalServerError, "Unknown error")
                        }
                        return@get
                    }
                )
            call.respondFile(image)
        }

        authenticate(Realm.HARBOUR_CONTROL) {
            get(
                path = "/harbour/location",
                builder = {
                    tags("harbourMaster")
                    description = "Get the assigned location for the authenticated harbor master"
                    request {
                        queryParameter<String>("timezone") {
                            description =
                                "Optional timezone ('Europe/Berlin'). Defaults to Ip address based timezone. Backup UTC."
                            required = false
                        }
                    }
                    response {
                        HttpStatusCode.OK to {
                            description = "Successful response with location details"
                            body<DetailedLocationDTO>()
                        }
                        HttpStatusCode.Unauthorized to {
                            description = "User is not a harbor master"
                        }
                        HttpStatusCode.NotFound to {
                            description = "No location assigned to this harbor master"
                        }
                    }
                }
            ) {
                val user = call.principal<LoggedInUser>()!!

                val tz = TimezonesService.getClientTimeZoneFromIPOrQueryParam(
                    call.parameters["timezone"],
                    call.request.origin.remoteAddress
                )

                call.respondKIO(LocationService.getHarborMasterAssignedLocation(user.id, tz))
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
                        queryParameter<String>("timezone") {
                            description =
                                "Optional timezone ('Europe/Berlin'). Defaults to Ip address based timezone. Backup UTC."
                            required = false
                        }
                        body<UpdateLocationRequest> {
                            example("Update Location Example") {
                                value = UpdateLocationRequest(
                                    name = "Updated Marina Test",
                                    description = "This is a new description for the marina.",
                                    address = "456 Pier Road",
                                    openingHours = "Mon-Fri: 09:00-17:00; Sat: 10:00-14:00",
                                    contact = Contact(
                                        phone = "1234567890",
                                        email = "info@marina.com",
                                        website = "https://marina-updated.com"
                                    ),
                                    image = ImageRequest(
                                        base64 = "/9j/4AAQSkZJRgABAQAAAQABAAD/2wBDAAIB...",
                                        contentType = "image/jpeg"
                                    ),
                                )
                            }
                        }
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
                val id = call.parameters["id"]?.toLongOrNull()
                    ?: return@put call.respondText("Missing or wrong id", status = HttpStatusCode.BadRequest)

                val user = call.principal<LoggedInUser>()!!
                val request = call.receive<UpdateLocationRequest>()


                val tz = TimezonesService.getClientTimeZoneFromIPOrQueryParam(
                    call.parameters["timezone"],
                    call.request.origin.remoteAddress
                )

                call.respondKIO(LocationService.updateLocationByID(user.id, id, request, tz))
            }

            delete(
                path = "/location/{id}/image",
                builder = {
                    tags("location")
                    description = "Delete a location"
                    request {
                        pathParameter<Long>("id") {
                            description = "The location ID (not the sensor ID)"
                        }
                    }
                    response {
                        HttpStatusCode.OK to {
                            body<Unit>()
                        }
                        HttpStatusCode.BadRequest to {
                            description = "Invalid parameters"
                        }
                    }
                }
            ) {
                val id = call.parameters["id"]?.toLongOrNull()
                    ?: return@delete call.respondText("Missing or wrong id", status = HttpStatusCode.BadRequest)

                call.respondKIO(LocationService.deleteLocationImage(id))
            }
        }
    }
}
