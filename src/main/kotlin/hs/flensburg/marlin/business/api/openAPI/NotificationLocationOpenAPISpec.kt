package hs.flensburg.marlin.business.api.openAPI

import NotificationLocationDTO
import hs.flensburg.marlin.business.api.notificationLocation.entity.CreateOrUpdateNotificationLocationRequest
import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.ktor.http.HttpStatusCode

object NotificationLocationOpenAPISpec {

    val getNotificationLocation: RouteConfig.() -> Unit = {
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

    val getAllNotificationsLocation: RouteConfig.() -> Unit = {
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

    val postNotificationLocation: RouteConfig.() -> Unit = {
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

    val deleteNotificationLocation: RouteConfig.() -> Unit = {
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

}