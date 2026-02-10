package hs.flensburg.marlin.business.api.openAPI

import hs.flensburg.marlin.business.api.subscription.entity.CancelSubscriptionRequest
import hs.flensburg.marlin.business.api.subscription.entity.CreatePortalSessionRequest
import hs.flensburg.marlin.business.api.subscription.entity.CreateSubscriptionRequest
import hs.flensburg.marlin.business.api.subscription.entity.PaymentSheetResponse
import hs.flensburg.marlin.business.api.subscription.entity.PortalSessionResponse
import hs.flensburg.marlin.business.api.subscription.entity.ReactivateSubscriptionRequest
import hs.flensburg.marlin.business.api.subscription.entity.SubscriptionStatusResponse
import hs.flensburg.marlin.business.api.subscription.entity.UserSubscriptionsResponse
import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.ktor.http.HttpStatusCode

object SubscriptionOpenAPISpec {

    val createSubscription: RouteConfig.() -> Unit = {
        tags("subscriptions")
        description = "Create a Stripe subscription and return PaymentSheet parameters. " +
                "Returns a PaymentIntent client secret (for immediate payment) or SetupIntent client secret " +
                "(for trials), along with an ephemeral key and customer ID for use with the Stripe mobile SDK PaymentSheet."
        securitySchemeNames("BearerAuth")

        request {
            body<CreateSubscriptionRequest> {
                description = "Subscription type (APP_NOTIFICATION or API_ACCESS)"
            }
        }

        response {
            HttpStatusCode.OK to {
                description = "Subscription created, PaymentSheet parameters returned"
                body<PaymentSheetResponse>()
            }
            HttpStatusCode.Conflict to {
                description = "User already has an active subscription of this type"
                body<String>()
            }
            HttpStatusCode.Unauthorized to {
                description = "User not authenticated"
                body<String>()
            }
        }
    }

    val createPortalSession: RouteConfig.() -> Unit = {
        tags("subscriptions")
        description = "Create a Stripe Customer Portal session for managing subscriptions. " +
                "Returns a URL to redirect the user to the Stripe billing portal."
        securitySchemeNames("BearerAuth")

        request {
            body<CreatePortalSessionRequest> {
                description = "Return URL after the user leaves the portal"
            }
        }

        response {
            HttpStatusCode.OK to {
                description = "Portal session created successfully"
                body<PortalSessionResponse>()
            }
            HttpStatusCode.NotFound to {
                description = "No Stripe customer found for this user"
                body<String>()
            }
            HttpStatusCode.Unauthorized to {
                description = "User not authenticated"
                body<String>()
            }
        }
    }

    val getStatus: RouteConfig.() -> Unit = {
        tags("subscriptions")
        description = "Get the current subscription status for both subscription types (APP_NOTIFICATION and API_ACCESS)."
        securitySchemeNames("BearerAuth")

        response {
            HttpStatusCode.OK to {
                description = "Subscription status for both types"
                body<UserSubscriptionsResponse>()
            }
            HttpStatusCode.Unauthorized to {
                description = "User not authenticated"
                body<String>()
            }
        }
    }

    val cancelSubscription: RouteConfig.() -> Unit = {
        tags("subscriptions")
        description = "Cancel a subscription at the end of the current billing period. " +
                "The subscription remains active until the period ends."
        securitySchemeNames("BearerAuth")

        request {
            body<CancelSubscriptionRequest> {
                description = "The subscription type to cancel"
            }
        }

        response {
            HttpStatusCode.OK to {
                description = "Subscription marked for cancellation"
                body<SubscriptionStatusResponse>()
            }
            HttpStatusCode.NotFound to {
                description = "No active subscription of this type found"
                body<String>()
            }
            HttpStatusCode.Unauthorized to {
                description = "User not authenticated"
                body<String>()
            }
        }
    }

    val reactivateSubscription: RouteConfig.() -> Unit = {
        tags("subscriptions")
        description = "Reactivate a subscription that was previously canceled but has not yet reached the end of its billing period."
        securitySchemeNames("BearerAuth")

        request {
            body<ReactivateSubscriptionRequest> {
                description = "The subscription type to reactivate"
            }
        }

        response {
            HttpStatusCode.OK to {
                description = "Subscription reactivated successfully"
                body<SubscriptionStatusResponse>()
            }
            HttpStatusCode.NotFound to {
                description = "No active subscription of this type found"
                body<String>()
            }
            HttpStatusCode.Conflict to {
                description = "Subscription is not pending cancellation"
                body<String>()
            }
            HttpStatusCode.Unauthorized to {
                description = "User not authenticated"
                body<String>()
            }
        }
    }

    val webhook: RouteConfig.() -> Unit = {
        tags("subscriptions")
        description = "Stripe webhook endpoint for subscription events. " +
                "Handles customer.subscription.created/updated/deleted and invoice.payment_failed events. " +
                "The webhook signature is verified to ensure authenticity."

        response {
            HttpStatusCode.OK to {
                description = "Webhook processed successfully"
                body<Map<String, Boolean>>()
            }
            HttpStatusCode.Unauthorized to {
                description = "Invalid webhook signature"
                body<String>()
            }
            HttpStatusCode.BadRequest to {
                description = "Missing signature header or invalid event"
                body<String>()
            }
        }
    }
}
