import hs.flensburg.marlin.business.api.notificationLocation.entity.CreateOrUpdateNotificationLocationRequest
import hs.flensburg.marlin.plugins.respondKIO
import io.github.smiley4.ktoropenapi.delete
import io.github.smiley4.ktoropenapi.get
import io.github.smiley4.ktoropenapi.post
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.routing.routing
import kotlin.text.toLong

fun Application.configureNotificationLocations() {
    routing {
        get(
            path = "/notification-locations/{id}",
            builder = {
                description = "Get a notification location by its ID "
                tags("notification-locations")
                request {
                    pathParameter<Long>("id") {
                        description = "ID of the notification location"
                    }
                }
                response {
                    HttpStatusCode.OK to {
                        body<NotificationLocationDTO>()
                    }
                    HttpStatusCode.NotFound to {
                        body<String>()
                    }
                }
            }
        ) {
            val id = call.parameters["id"]!!.toLong()
            call.respondKIO(NotificationLocationsService.getNotificationLocation(id))
        }

        get(
            path = "/notification-locations/all/{locationId}",
            builder = {
                description = "Get all notification locations from a location by its ID"
                tags("notification-locations")
                request {
                    pathParameter<Long>("locationId") {
                        description = "ID of the location"
                    }
                }
                response {
                    HttpStatusCode.OK to {
                        body<NotificationLocationDTO>()
                    }
                    HttpStatusCode.NotFound to {
                        body<String>()
                    }
                }
            }
        ) {
            val locationId = call.parameters["locationId"]!!.toLong()
            call.respondKIO(NotificationLocationsService.getAllNotificationLocationsFromLocation(locationId))
        }

        post(
            path = "/notification-locations",
            builder = {
                description = "Create a notification location"
                tags("notification-locations")
                request {
                    body<CreateOrUpdateNotificationLocationRequest>()
                }
                response {
                    HttpStatusCode.Created to {
                        body<NotificationLocationDTO>()
                    }
                    HttpStatusCode.BadRequest to {
                        body<String>()
                    }
                }
            }
        ) {
            val request = call.receive<CreateOrUpdateNotificationLocationRequest>()
            val allUserLocationsByLocationId = UserLocationsService.getAllUserLocationsByLocationId(request.locationId)
            FirebaseNotificationSender.sendNotification(
                token="",
                title="",
                message=""
            )
            call.respondKIO(NotificationLocationsService.create(request))
        }

        delete(
            path = "/notification-locations/{id}",
            builder = {
                description = "Delete a notification location by ID."
                tags("notification-locations")
                request {
                    pathParameter<Long>("id") {
                        description = "ID of the notification location"
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
            call.respondKIO(NotificationLocationsService.delete(id))
        }
    }
}