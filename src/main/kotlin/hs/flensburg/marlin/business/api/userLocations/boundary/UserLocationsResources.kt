import hs.flensburg.marlin.business.api.userLocations.entity.CreateOrUpdateUserLocationRequest
import hs.flensburg.marlin.plugins.respondKIO
import io.github.smiley4.ktoropenapi.delete
import io.github.smiley4.ktoropenapi.get
import io.github.smiley4.ktoropenapi.post
import io.github.smiley4.ktoropenapi.put
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.routing.routing
import kotlin.text.toLong

fun Application.configureUserLocations() {
    routing {
        get(
            path = "/user-locations/{id}",
            builder = {
                description = "Get a user location by its ID "
                tags("user-locations")
                request {
                    pathParameter<Long>("id") {
                        description = "ID of the user location"
                    }
                }
                response {
                    HttpStatusCode.OK to {
                        body<UserLocationDTO>()
                    }
                    HttpStatusCode.NotFound to {
                        body<String>()
                    }
                }
            }
        ) {
            val id = call.parameters["id"]!!.toLong()
            call.respondKIO(UserLocationsService.getUserLocation(id))
        }

        get(
            path = "/user-locations/user/{userId}",
            builder = {
                description = "Get all user locations from a user by the user ID"
                tags("user-locations")
                request {
                    pathParameter<Long>("userId") {
                        description = "ID of the user"
                    }
                }
                response {
                    HttpStatusCode.OK to {
                        body<UserLocationDTO>()
                    }
                    HttpStatusCode.NotFound to {
                        body<String>()
                    }
                }
            }
        ) {
            val userId = call.parameters["userId"]!!.toLong()
            call.respondKIO(UserLocationsService.getAllUserLocationsFromUser(userId))
        }

        post(
            path = "/user-locations",
            builder = {
                description = "Create a user location"
                tags("user-locations")
                request {
                    body<CreateOrUpdateUserLocationRequest>()
                }
                response {
                    HttpStatusCode.Created to {
                        body<UserLocationDTO>()
                    }
                    HttpStatusCode.BadRequest to {
                        body<String>()
                    }
                }
            }
        ) {
            val request = call.receive<CreateOrUpdateUserLocationRequest>()
            call.respondKIO(UserLocationsService.create(request))
        }

        put(
            path = "/user-locations/{id}",
            builder = {
                description = "Update a user location by its ID"
                tags("user-locations")
                request {
                    body<CreateOrUpdateUserLocationRequest>()
                }
                response {
                    HttpStatusCode.OK to {
                        body<UserLocationDTO>()
                    }
                    HttpStatusCode.NotFound to {
                        body<String>()
                    }
                }
            }
        ) {
            val id = call.parameters["id"]!!.toLong()
            val request = call.receive<CreateOrUpdateUserLocationRequest>()
            call.respondKIO(UserLocationsService.update(id, request))
        }

        delete(
            path = "/user-locations/{id}",
            builder = {
                description = "Delete a user location by ID."
                tags("user-locations")
                request {
                    pathParameter<Long>("id") {
                        description = "ID of the user location"
                    }
                }
                response {
                    HttpStatusCode.NoContent to {}
                    HttpStatusCode.NotFound to {
                        body<String>()
                    }
                }
            }
        ) {
            val id = call.parameters["id"]!!.toLong()
            call.respondKIO(UserLocationsService.delete(id))
        }
    }
}