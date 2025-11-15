package hs.flensburg.marlin.business.api.openAPI

import hs.flensburg.marlin.business.api.payment.entity.CreatePaymentIntentRequest
import hs.flensburg.marlin.business.api.payment.entity.CreatePaymentIntentResponse
import hs.flensburg.marlin.business.api.payment.entity.WebhookResponse
import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.ktor.http.HttpStatusCode

object PaymentOpenAPISpec {

    val createIntent: RouteConfig.() -> Unit = {
        tags("payments")
        description = "Create a Stripe payment intent for the authenticated user. " +
                "Returns a client secret that can be used with Stripe.js to complete the payment on the frontend."

        securitySchemeNames("BearerAuth")

        request {
            body<CreatePaymentIntentRequest> {
                description = "Payment intent creation request containing amount and currency"
            }
        }

        response {
            HttpStatusCode.OK to {
                description = "Payment intent created successfully"
                body<CreatePaymentIntentResponse>()
            }
            HttpStatusCode.BadRequest to {
                description = "Invalid request (e.g., negative amount)"
                body<String>()
            }
            HttpStatusCode.Unauthorized to {
                description = "User not authenticated"
                body<String>()
            }
        }
    }

    val webhook: RouteConfig.() -> Unit = {
        tags("payments")
        description = "Stripe webhook endpoint for payment events. " +
                "This endpoint is called by Stripe when payment events occur (succeeded, failed, canceled). " +
                "The webhook signature is verified to ensure authenticity."

        response {
            HttpStatusCode.OK to {
                description = "Webhook processed successfully"
                body<WebhookResponse>()
            }
            HttpStatusCode.Unauthorized to {
                description = "Invalid webhook signature"
                body<String>()
            }
            HttpStatusCode.BadRequest to {
                description = "Invalid webhook event or missing signature header"
                body<String>()
            }
        }
    }
}
