package hs.flensburg.marlin.business.api.notifications.boundary

import hs.flensburg.marlin.business.api.notifications.FirebaseNotificationSender
import hs.flensburg.marlin.business.api.notifications.entity.TestNotificationRequest
import hs.flensburg.marlin.business.api.users.entity.UserProfileResponse
import io.github.smiley4.ktoropenapi.post
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.routing.routing

fun Application.configureNotifications() {
    routing {
        post(
            path = "/test-notification",
            builder = {
                description = "Test notification"
                tags("test-notification")
                request {
                    body<TestNotificationRequest>()
                }
                response {
                    HttpStatusCode.OK to {
                        body<TestNotificationRequest>()
                    }
                    HttpStatusCode.BadRequest to {
                        body<String>()
                    }
                }
            }
        ) {
            val request = call.receive<TestNotificationRequest>()
            FirebaseNotificationSender.sendNotification(request.FCMtoken, request.title, request.message)
        }
    }
}