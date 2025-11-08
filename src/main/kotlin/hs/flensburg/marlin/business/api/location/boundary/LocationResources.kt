package hs.flensburg.marlin.business.api.location.boundary

import de.lambda9.tailwind.core.KIO.Companion.unsafeRunSync
import hs.flensburg.marlin.business.api.auth.entity.LoggedInUser
import hs.flensburg.marlin.business.api.location.entity.DetailedLocationDTO
import hs.flensburg.marlin.business.api.location.entity.UpdateLocationRequest
import hs.flensburg.marlin.plugins.Realm
import hs.flensburg.marlin.plugins.authenticate
import hs.flensburg.marlin.plugins.kioEnv
import hs.flensburg.marlin.plugins.receiveImageFile
import hs.flensburg.marlin.plugins.respondKIO
import io.github.smiley4.ktoropenapi.delete
import io.github.smiley4.ktoropenapi.get
import io.github.smiley4.ktoropenapi.post
import io.github.smiley4.ktoropenapi.put
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.server.application.Application
import io.ktor.server.auth.principal
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
            val id = call.parameters["id"]?.toLongOrNull()
                ?: return@get call.respondText("Missing or wrong id", status = HttpStatusCode.BadRequest)

            call.respondKIO(LocationService.getLocationByID(id))
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
                        val e = error.failures().first().toApiError()
                        call.respond(e.statusCode, e.message)
                        return@get
                    }
                )
            call.respondFile(image)
        }

        authenticate(Realm.HARBOUR_CONTROL) {

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
                val id = call.parameters["id"]?.toLongOrNull()
                    ?: return@put call.respondText("Missing or wrong id", status = HttpStatusCode.BadRequest)

                val user = call.principal<LoggedInUser>()!!
                val request = call.receive<UpdateLocationRequest>()
                call.respondKIO(LocationService.updateLocationByID(user.id, id, request))
            }



            post("/location/{id}/image", {
                description = "Create the image of a location"
                tags("location")
                request {
                    pathParameter<Long>("id") {
                        description = "The location ID"
                    }
                    multipartBody {
                        part<PartData.FileItem>("image") {
                            required = true
                        }
                    }
                }
                response {
                    HttpStatusCode.OK to {
                        body<Unit>()
                    }
                    HttpStatusCode.NotFound to {
                        body<String>()
                    }
                }
            }) {
                val id = call.parameters["id"]?.toLongOrNull()
                    ?: return@post call.respondText("Missing or wrong id", status = HttpStatusCode.BadRequest)

                val user = call.principal<LoggedInUser>()!!
                val (imageBytes, contentType) = call.receiveImageFile()
                call.respondKIO(LocationService.createLocationImage(user.id, id, imageBytes, contentType))
            }

            put("/location/{id}/image", {
                description = "Update the image of a location"
                tags("location")
                request {
                    pathParameter<Long>("id") {
                        description = "The location ID"
                    }
                    multipartBody {
                        part<PartData.FileItem>("image") {
                            required = true
                        }
                    }
                }
                response {
                    HttpStatusCode.OK to {
                        body<Unit>()
                    }
                    HttpStatusCode.NotFound to {
                        body<String>()
                    }
                }
            }) {
                val id = call.parameters["id"]?.toLongOrNull()
                    ?: return@put call.respondText("Missing or wrong id", status = HttpStatusCode.BadRequest)

                val user = call.principal<LoggedInUser>()!!
                val (imageBytes, contentType) = call.receiveImageFile()
                call.respondKIO(LocationService.updateLocationImage(user.id, id,imageBytes, contentType))
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