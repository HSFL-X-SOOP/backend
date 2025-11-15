package hs.flensburg.marlin.business.api.notificationMeasurementRule.boundary

import hs.flensburg.marlin.business.api.notificationMeasurementRule.entity.CreateOrUpdateNotificationMeasurementRuleRequest
import hs.flensburg.marlin.business.api.notificationMeasurementRule.entity.NotificationMeasurementRuleDTO
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

fun Application.configureNotificationMeasurementRules() {
    routing {
        get(
            path = "/notification-measurement-rules/{id}",
            builder = {
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
        ) {
            val id = call.parameters["id"]!!.toLong()
            call.respondKIO(NotificationMeasurementRuleService.getNotificationMeasurementRule(id))
        }

        get(
            path = "/notification-measurement-rules/user/{userId}",
            builder = {
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
        ) {
            val userId = call.parameters["userId"]!!.toLong()
            call.respondKIO(NotificationMeasurementRuleService.getAllNotificationMeasurementRulesFromUser(userId))
        }

        post(
            path = "/notification-measurement-rules",
            builder = {
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
        ) {
            val request = call.receive<CreateOrUpdateNotificationMeasurementRuleRequest>()
            call.respondKIO(NotificationMeasurementRuleService.createRule(request))
        }

        put(
            path = "/notification-measurement-rules/{id}",
            builder = {
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
        ) {
            val id = call.parameters["id"]!!.toLong()
            val request = call.receive<CreateOrUpdateNotificationMeasurementRuleRequest>()
            call.respondKIO(NotificationMeasurementRuleService.updateRule(id, request))
        }

        delete(
            path = "/notification-measurement-rules/{id}",
            builder = {
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
        ) {
            val id = call.parameters["id"]!!.toLong()
            call.respondKIO(NotificationMeasurementRuleService.deleteRule(id))
        }
    }
}