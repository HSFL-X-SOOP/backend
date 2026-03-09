package hs.flensburg.marlin.business.api.openAPI

import hs.flensburg.marlin.business.api.notificationMeasurementRule.entity.CreateOrUpdateNotificationMeasurementRuleRequest
import hs.flensburg.marlin.business.api.notificationMeasurementRule.entity.NotificationMeasurementRuleDTO
import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.ktor.http.HttpStatusCode

object NotificationMeasurementOpenAPISpec {

    val getNotificationMeasurementRule: RouteConfig.() -> Unit = {
        description = "Get a notification measurement rule by its ID "
        tags("notification-measurement-rules")
        request {
            pathParameter<Long>("id") {
                description = "ID of the notification measurement rule"
            }
        }
        response {
            HttpStatusCode.OK to {
                body<NotificationMeasurementRuleDTO>()
            }
            HttpStatusCode.NotFound to {
                body<String>()
            }
        }
    }

    val getNotificationMeasurementRuleUser: RouteConfig.() -> Unit = {
        description = "Get all notification measurement rules from a user by the user ID"
        tags("notification-measurement-rules")
        request {
            pathParameter<Long>("userId") {
                description = "ID of the user"
            }
        }
        response {
            HttpStatusCode.OK to {
                body<NotificationMeasurementRuleDTO>()
            }
            HttpStatusCode.NotFound to {
                body<String>()
            }
        }
    }

    val getNotificationMeasurementRuleUserLocation: RouteConfig.() -> Unit = {
        description = "Get all notification measurement rules from a user by the user ID and Location ID"
        tags("notification-measurement-rules")
        request {
            pathParameter<Long>("userId") {
                description = "ID of the user"
            }
            pathParameter<Long>("locationId") {
                description = "ID of the location"
            }
        }
        response {
            HttpStatusCode.OK to {
                body<NotificationMeasurementRuleDTO>()
            }
            HttpStatusCode.NotFound to {
                body<String>()
            }
        }
    }

    val getNotificationMeasurementRuleSpecific: RouteConfig.() -> Unit = {
        description = "Get all notification measurement rules from a user by the user ID"
        tags("notification-measurement-rules")
        request {
            pathParameter<Long>("userId") {
                description = "ID of the user"
            }
            pathParameter<Long>("locationId") {
                description = "ID of the location"
            }
            pathParameter<Long>("measurementTypeId") {
                description = "ID of the measurement_type"
            }
        }
        response {
            HttpStatusCode.OK to {
                body<NotificationMeasurementRuleDTO>()
            }
            HttpStatusCode.NotFound to {
                body<String>()
            }
        }
    }

    val postNotificationMeasurementRule: RouteConfig.() -> Unit = {
        description = "Create a notification measurement rule"
        tags("notification-measurement-rules")
        request {
            body<CreateOrUpdateNotificationMeasurementRuleRequest>()
        }
        response {
            HttpStatusCode.Created to {
                body<NotificationMeasurementRuleDTO>()
            }
            HttpStatusCode.BadRequest to {
                body<String>()
            }
        }
    }

    val putNotificationMeasurementRule: RouteConfig.() -> Unit = {
        description = "Update a notification measurement rule by its ID"
        tags("notification-measurement-rules")
        request {
            body<CreateOrUpdateNotificationMeasurementRuleRequest>()
        }
        response {
            HttpStatusCode.OK to {
                body<NotificationMeasurementRuleDTO>()
            }
            HttpStatusCode.NotFound to {
                body<String>()
            }
        }
    }

    val deleteNotificationMeasurementRule: RouteConfig.() -> Unit = {
        description = "Delete a notification measurement rule by ID."
        tags("notification-measurement-rules")
        request {
            pathParameter<Long>("id") {
                description = "ID of the user device entry"
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