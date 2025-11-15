package hs.flensburg.marlin.business.api.payment.control

import com.stripe.exception.SignatureVerificationException
import com.stripe.exception.StripeException
import com.stripe.model.Event
import com.stripe.model.PaymentIntent
import com.stripe.net.Webhook
import de.lambda9.tailwind.core.KIO
import hs.flensburg.marlin.business.ApiError
import hs.flensburg.marlin.business.App
import hs.flensburg.marlin.business.JEnv
import hs.flensburg.marlin.business.ServiceLayerError
import hs.flensburg.marlin.business.api.payment.entity.PaymentIntentData
import hs.flensburg.marlin.database.generated.enums.PaymentStatus
import io.github.oshai.kotlinlogging.KotlinLogging

object StripeService {
    sealed class Error(private val message: String) : ServiceLayerError {
        object InvalidWebhookSignature : Error("Invalid webhook signature")
        object PaymentIntentNotFound : Error("Payment intent not found")
        data class StripeApiError(val detail: String) : Error("Stripe API error: $detail")
        data class InvalidAmount(val amount: Long) : Error("Invalid payment amount: $amount")

        override fun toApiError(): ApiError {
            return when (this) {
                is InvalidWebhookSignature -> ApiError.Unauthorized(message)
                is PaymentIntentNotFound -> ApiError.NotFound(message)
                is StripeApiError -> ApiError.BadGateway(message)
                is InvalidAmount -> ApiError.BadRequest(message)
            }
        }
    }

    private val logger = KotlinLogging.logger {}

    fun verifyWebhookSignature(payload: String, signature: String): App<Error, Event> = KIO.comprehension {
        val (_, env) = !KIO.access<JEnv>()

        try {
            KIO.ok(
                Webhook.constructEvent(
                    payload,
                    signature,
                    env.config.stripe.webhookSecret
                )
            )
        } catch (e: SignatureVerificationException) {
            logger.error(e) { "Invalid webhook signature: ${e.message}" }
            KIO.fail(Error.InvalidWebhookSignature)
        } catch (e: Exception) {
            logger.error(e) { "Error verifying webhook signature: ${e.message}" }
            KIO.fail(Error.StripeApiError(e.message ?: "Unknown error"))
        }
    }

    fun handlePaymentIntentEvent(event: Event): App<Error, PaymentIntentData> = KIO.comprehension {
        try {
            val paymentIntent = event.dataObjectDeserializer.`object`.get() as? PaymentIntent

            if (paymentIntent == null) {
                KIO.fail(Error.PaymentIntentNotFound)
            } else {
                KIO.ok(
                    PaymentIntentData(
                        id = paymentIntent.id,
                        amount = paymentIntent.amount,
                        currency = paymentIntent.currency,
                        status = mapStripeStatusToPaymentStatus(paymentIntent.status),
                        metadata = paymentIntent.metadata ?: emptyMap(),
                        eventType = event.type
                    )
                )
            }
        } catch (e: Exception) {
            logger.error(e) { "Error handling payment intent event: ${e}" }
            KIO.fail(Error.StripeApiError(e.message ?: "Unknown error"))
        }
    }

    fun createPaymentIntent(
        amount: Long,
        currency: String,
        userId: String
    ): App<Error, PaymentIntent> = KIO.comprehension {
        !KIO.failOn(amount <= 0) { Error.InvalidAmount(amount) }

        try {
            val params = mapOf(
                "amount" to amount,
                "currency" to currency,
                "metadata" to mapOf("user_id" to userId)
            )

            val paymentIntent = PaymentIntent.create(params)
            logger.info { "Created payment intent: ${paymentIntent.id} for user: $userId" }
            KIO.ok(paymentIntent)
        } catch (e: StripeException) {
            logger.error(e) { "Stripe API error: ${e.message}" }
            KIO.fail(Error.StripeApiError(e.message ?: "Unknown Stripe error"))
        } catch (e: Exception) {
            logger.error(e) { "Error creating payment intent: ${e.message}" }
            KIO.fail(Error.StripeApiError(e.message ?: "Unknown error"))
        }
    }

    fun retrievePaymentIntent(paymentIntentId: String): App<Error, PaymentIntent> = KIO.comprehension {
        try {
            val paymentIntent = PaymentIntent.retrieve(paymentIntentId)
            logger.info { "Retrieved payment intent: $paymentIntentId" }
            KIO.ok(paymentIntent)
        } catch (e: StripeException) {
            logger.error(e) { "Error retrieving payment intent: ${e.message}" }
            KIO.fail(Error.PaymentIntentNotFound)
        } catch (e: Exception) {
            logger.error(e) { "Error retrieving payment intent: ${e.message}" }
            KIO.fail(Error.StripeApiError(e.message ?: "Unknown error"))
        }
    }

    private fun mapStripeStatusToPaymentStatus(stripeStatus: String): PaymentStatus {
        return when (stripeStatus) {
            "succeeded" -> PaymentStatus.SUCCEEDED
            "canceled" -> PaymentStatus.CANCELED
            "requires_action", "requires_confirmation", "requires_payment_method" -> PaymentStatus.REQUIRES_ACTION
            "processing", "created" -> PaymentStatus.PENDING
            else -> PaymentStatus.FAILED
        }
    }
}