package hs.flensburg.marlin.business.api.payment.boundary

import com.stripe.Stripe
import hs.flensburg.marlin.Config
import hs.flensburg.marlin.business.api.auth.entity.LoggedInUser
import hs.flensburg.marlin.business.api.openAPI.PaymentOpenAPISpec
import hs.flensburg.marlin.business.api.payment.entity.CreatePaymentIntentRequest
import hs.flensburg.marlin.plugins.Realm
import hs.flensburg.marlin.plugins.authenticate
import hs.flensburg.marlin.plugins.respondKIO
import io.github.smiley4.ktoropenapi.post
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.routing.routing

fun Application.configurePaymentResources(config: Config) {
    Stripe.apiKey = config.stripe.secretKey

    routing {
        authenticate(Realm.COMMON) {
            post("/payments/create-intent", PaymentOpenAPISpec.createIntent) {
                val user = call.principal<LoggedInUser>()!!
                val intent = call.receive<CreatePaymentIntentRequest>()

                call.respondKIO(
                    PaymentService.createPaymentIntent(
                        userId = user.id,
                        paymentIntent = intent
                    )
                )
            }
        }

        post("/payments/webhook", PaymentOpenAPISpec.webhook) {
            val payload = call.receiveText()
            val signature = call.request.headers["Stripe-Signature"] ?: return@post call.respond(
                HttpStatusCode.BadRequest,
                "Missing signature header"
            )

            call.respondKIO(
                PaymentService.processWebhook(payload, signature)
            )
        }
    }
}