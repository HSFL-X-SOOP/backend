package hs.flensburg.marlin.business.api.location.boundary

import de.lambda9.tailwind.core.KIO
import hs.flensburg.marlin.business.api.location.entity.DetailedLocationDTO
import hs.flensburg.marlin.business.api.location.entity.UpdateLocationRequest
import hs.flensburg.marlin.plugins.respondKIO
import io.github.smiley4.ktoropenapi.get
import io.github.smiley4.ktoropenapi.post
import io.github.smiley4.ktoropenapi.put
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.request.receiveMultipart
import io.ktor.server.routing.routing
import io.ktor.utils.io.readRemaining
import kotlinx.io.readByteArray

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
            val locationID = call.parameters["id"]?.toLongOrNull()

            if (locationID == null) {
                call.respondKIO(KIO.ok("LocationID fehlt oder ungültig"))
                return@get
            }

            call.respondKIO(LocationService.getLocationImage(locationID))
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
            val locationID = call.parameters["id"]?.toLongOrNull()

            if (locationID == null) {
                call.respondKIO(KIO.ok("LocationID fehlt oder ungültig"))
                return@post
            }

            var imageBytes: ByteArray? = null
            var contentType: String? = null

            val multipart = call.receiveMultipart()
            multipart.forEachPart { part ->
                if (part is PartData.FileItem && part.name == "image") {
                    contentType = part.contentType?.toString()

                    if (contentType?.startsWith("image/") != true) {
                        part.dispose()
                        call.respondKIO(KIO.ok("Only image files are allowed (received: $contentType)"))
                        return@forEachPart
                    }

                    imageBytes = part.provider().readRemaining().readByteArray()
                }
                part.dispose()
            }

            if (imageBytes == null) {
                call.respondKIO(KIO.ok("No image file provided"))
                return@post
            }


            call.respondKIO(LocationService.createLocationImage(locationID, imageBytes))
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
            val locationID = call.parameters["id"]?.toLongOrNull()

            if (locationID == null) {
                call.respondKIO(KIO.ok("LocationID fehlt oder ungültig"))
                return@put
            }

            var imageBytes: ByteArray? = null
            var contentType: String? = null

            val multipart = call.receiveMultipart()
            multipart.forEachPart { part ->
                if (part is PartData.FileItem && part.name == "image") {
                    contentType = part.contentType?.toString()

                    if (contentType?.startsWith("image/") != true) {
                        part.dispose()
                        call.respondKIO(KIO.ok("Only image files are allowed (received: $contentType)"))
                        return@forEachPart
                    }

                    imageBytes = part.provider().readRemaining().readByteArray()
                }
                part.dispose()
            }

            if (imageBytes == null) {
                call.respondKIO(KIO.ok("No image file provided"))
                return@put
            }

            call.respondKIO(LocationService.updateLocationImage(locationID, imageBytes))
        }

    }
}