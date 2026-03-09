package hs.flensburg.marlin.business.api.notificationMeasurementRule.boundary

import hs.flensburg.marlin.business.api.notificationMeasurementRule.entity.CreateOrUpdateNotificationMeasurementRuleRequest
import hs.flensburg.marlin.business.api.openAPI.NotificationMeasurementOpenAPISpec
import hs.flensburg.marlin.plugins.respondKIO
import io.github.smiley4.ktoropenapi.delete
import io.github.smiley4.ktoropenapi.get
import io.github.smiley4.ktoropenapi.post
import io.github.smiley4.ktoropenapi.put
import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.routing.routing

fun Application.configureNotificationMeasurementRules() {
    routing {
        get(
            "/notification-measurement-rules/{id}",
            NotificationMeasurementOpenAPISpec.getNotificationMeasurementRule
        ) {
            val id = call.parameters["id"]!!.toLong()
            call.respondKIO(NotificationMeasurementRuleService.getNotificationMeasurementRule(id))
        }

        get(
            "/notification-measurement-rules/user/{userId}",
            NotificationMeasurementOpenAPISpec.getNotificationMeasurementRuleUser
        ) {
            val userId = call.parameters["userId"]!!.toLong()
            call.respondKIO(NotificationMeasurementRuleService.getAllNotificationMeasurementRulesFromUser(userId))
        }

        get(
            "/notification-measurement-rules/user/{userId}/location/{locationId}",
            NotificationMeasurementOpenAPISpec.getNotificationMeasurementRuleUserLocation
        ) {
            val userId = call.parameters["userId"]!!.toLong()
            val locationId = call.parameters["locationId"]!!.toLong()
            call.respondKIO(
                NotificationMeasurementRuleService.getAllotificationMeasurementRuleByUserIdAndLocationId(
                    userId,
                    locationId
                )
            )
        }

        get(
            "/notification-measurement-rules/user/{userId}/location/{locationId}/measurementTypeId/{measurementTypeId}",
            NotificationMeasurementOpenAPISpec.getNotificationMeasurementRuleSpecific
        ) {
            val userId = call.parameters["userId"]!!.toLong()
            val locationId = call.parameters["locationId"]!!.toLong()
            val measurementTypeId = call.parameters["measurementTypeId"]!!.toLong()
            call.respondKIO(
                NotificationMeasurementRuleService.getNotificationMeasurementRule(
                    userId,
                    locationId,
                    measurementTypeId
                )
            )
        }

        post(
            "/notification-measurement-rules",
            NotificationMeasurementOpenAPISpec.postNotificationMeasurementRule
        ) {
            val request = call.receive<CreateOrUpdateNotificationMeasurementRuleRequest>()
            call.respondKIO(NotificationMeasurementRuleService.createRule(request))
        }

        put(
            "/notification-measurement-rules/{id}",
            NotificationMeasurementOpenAPISpec.putNotificationMeasurementRule
        ) {
            val id = call.parameters["id"]!!.toLong()
            val request = call.receive<CreateOrUpdateNotificationMeasurementRuleRequest>()
            call.respondKIO(NotificationMeasurementRuleService.updateRule(id, request))
        }

        delete(
            "/notification-measurement-rules/{id}",
            NotificationMeasurementOpenAPISpec.deleteNotificationMeasurementRule
        ) {
            val id = call.parameters["id"]!!.toLong()
            call.respondKIO(NotificationMeasurementRuleService.deleteRule(id))
        }
    }
}