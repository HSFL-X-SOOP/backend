package hs.flensburg.marlin.business.api.subscription.boundary

import hs.flensburg.marlin.Config
import hs.flensburg.marlin.business.api.auth.entity.LoggedInUser
import hs.flensburg.marlin.business.api.openAPI.SubscriptionOpenAPISpec
import hs.flensburg.marlin.business.api.subscription.entity.CancelSubscriptionRequest
import hs.flensburg.marlin.business.api.subscription.entity.CreatePortalSessionRequest
import hs.flensburg.marlin.business.api.subscription.entity.CreateSubscriptionRequest
import hs.flensburg.marlin.business.api.subscription.entity.ReactivateSubscriptionRequest
import hs.flensburg.marlin.plugins.Realm
import hs.flensburg.marlin.plugins.authenticate
import hs.flensburg.marlin.plugins.respondKIO
import io.github.smiley4.ktoropenapi.get
import io.github.smiley4.ktoropenapi.post
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.routing.routing

fun Application.configureSubscriptionResources() {
    routing {
        authenticate(Realm.COMMON) {
            post("/subscriptions/create", SubscriptionOpenAPISpec.createSubscription) {
                val user = call.principal<LoggedInUser>()!!
                val request = call.receive<CreateSubscriptionRequest>()

                call.respondKIO(
                    SubscriptionService.createSubscription(user.id, user.email, request)
                )
            }

            post("/subscriptions/portal", SubscriptionOpenAPISpec.createPortalSession) {
                val user = call.principal<LoggedInUser>()!!
                val request = call.receive<CreatePortalSessionRequest>()

                call.respondKIO(
                    SubscriptionService.createPortalSession(user.id, request)
                )
            }

            get("/subscriptions/status", SubscriptionOpenAPISpec.getStatus) {
                val user = call.principal<LoggedInUser>()!!

                call.respondKIO(
                    SubscriptionService.getSubscriptionStatus(user.id)
                )
            }

            post("/subscriptions/cancel", SubscriptionOpenAPISpec.cancelSubscription) {
                val user = call.principal<LoggedInUser>()!!
                val request = call.receive<CancelSubscriptionRequest>()

                call.respondKIO(
                    SubscriptionService.cancelSubscription(user.id, request)
                )
            }

            post("/subscriptions/reactivate", SubscriptionOpenAPISpec.reactivateSubscription) {
                val user = call.principal<LoggedInUser>()!!
                val request = call.receive<ReactivateSubscriptionRequest>()

                call.respondKIO(
                    SubscriptionService.reactivateSubscription(user.id, request)
                )
            }
        }

        post("/subscriptions/webhook", SubscriptionOpenAPISpec.webhook) {
            val payload = call.receiveText()
            val signature = call.request.headers["Stripe-Signature"]
                ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing signature header")

            call.respondKIO(
                SubscriptionService.processWebhook(payload, signature)
            )
        }
    }
}
