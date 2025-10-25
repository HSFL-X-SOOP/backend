package hs.flensburg.marlin.business.api.location.boundary

import hs.flensburg.marlin.business.api.location.entity.DetailedLocationDTO
import hs.flensburg.marlin.business.api.location.entity.UpdateLocationRequest
import hs.flensburg.marlin.plugins.respondKIO
import io.github.smiley4.ktoropenapi.get
import io.github.smiley4.ktoropenapi.post
import io.github.smiley4.ktoropenapi.put
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.server.application.Application
import io.ktor.server.request.receive
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

            val request = call.receive<UpdateLocationRequest>()

            call.respondKIO(LocationService.updateLocationByID(id, request))
        }

        get(
            path = "/location/{id}/image",
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

            call.respondKIO(LocationService.getLocationImage(id))
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

            try {
                val imageBytes = call.receiveImageFile()
                call.respondKIO(LocationService.createLocationImage(id, imageBytes))
            } catch (e: BadRequestException) {
                call.respondText(e.message ?: "Missing image file", status = HttpStatusCode.BadRequest)
            } catch (e: UnsupportedMediaTypeException) {
                call.respondText(e.message ?: "Unsupported media type", status = HttpStatusCode.UnsupportedMediaType)
            }
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

            try {
                val imageBytes = call.receiveImageFile()
                call.respondKIO(LocationService.updateLocationImage(id, imageBytes))
            } catch (e: BadRequestException) {
                call.respondText(e.message ?: "Missing image file", status = HttpStatusCode.BadRequest)
            } catch (e: UnsupportedMediaTypeException) {
                call.respondText(e.message ?: "Unsupported media type", status = HttpStatusCode.UnsupportedMediaType)
            }
        }
    }
}