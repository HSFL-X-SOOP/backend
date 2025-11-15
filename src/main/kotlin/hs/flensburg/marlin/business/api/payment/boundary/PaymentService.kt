package hs.flensburg.marlin.business.api.payment.boundary

import com.stripe.model.Event
import de.lambda9.tailwind.core.KIO
import de.lambda9.tailwind.core.extensions.kio.onNullFail
import de.lambda9.tailwind.core.extensions.kio.orDie
import de.lambda9.tailwind.jooq.transact
import hs.flensburg.marlin.business.ApiError
import hs.flensburg.marlin.business.App
import hs.flensburg.marlin.business.ServiceLayerError
import hs.flensburg.marlin.business.api.payment.control.PaymentRepository
import hs.flensburg.marlin.business.api.payment.control.StripeService
import hs.flensburg.marlin.business.api.payment.entity.CreatePaymentIntentRequest
import hs.flensburg.marlin.business.api.payment.entity.CreatePaymentIntentResponse
import hs.flensburg.marlin.business.api.payment.entity.WebhookResponse
import hs.flensburg.marlin.database.generated.tables.pojos.Payment
import io.github.oshai.kotlinlogging.KotlinLogging

object PaymentService {
    sealed class Error(private val message: String) : ServiceLayerError {
        object InvalidSignature : Error("Invalid webhook signature")
        object PaymentNotFound : Error("Payment not found")
        object MissingUserId : Error("Missing user_id in payment metadata")
        object InvalidUserId : Error("Invalid user_id format")
        data class InvalidAmount(val amount: Long) : Error("Invalid payment amount: $amount")
        data class InvalidEventType(val eventType: String) : Error("Invalid event type: $eventType")
        data class StripeError(val detail: String) : Error("Stripe error: $detail")

        override fun toApiError(): ApiError {
            return when (this) {
                is InvalidSignature -> ApiError.Unauthorized(message)
                is PaymentNotFound -> ApiError.NotFound(message)
                is MissingUserId -> ApiError.BadRequest(message)
                is InvalidUserId -> ApiError.BadRequest(message)
                is InvalidAmount -> ApiError.BadRequest(message)
                is InvalidEventType -> ApiError.BadRequest(message)
                is StripeError -> ApiError.BadRequest(message)
            }
        }
    }

    private val logger = KotlinLogging.logger {}

    fun createPaymentIntent(
        userId: Long,
        paymentIntent: CreatePaymentIntentRequest
    ): App<ServiceLayerError, CreatePaymentIntentResponse> = KIO.comprehension {
        !KIO.failOn(paymentIntent.amount <= 0) { Error.InvalidAmount(paymentIntent.amount) }

        val paymentIntent = !StripeService.createPaymentIntent(
            amount = paymentIntent.amount,
            currency = paymentIntent.currency,
            userId = userId.toString()
        )

        logger.info { "Created payment intent ${paymentIntent.id} for user $userId with amount ${paymentIntent.amount} ${paymentIntent.currency}" }

        KIO.ok(
            CreatePaymentIntentResponse(
                clientSecret = paymentIntent.clientSecret,
                paymentIntentId = paymentIntent.id,
                amount = paymentIntent.amount,
                currency = paymentIntent.currency
            )
        )
    }.transact()

    fun processWebhook(payload: String, signature: String): App<Error, WebhookResponse> = KIO.comprehension {
        val event = !StripeService.verifyWebhookSignature(payload, signature)
            .mapError { Error.InvalidSignature }

        logger.info { "Processing webhook event: ${event.type}" }

        val payment = when (event.type) {
            "payment_intent.succeeded",
            "payment_intent.payment_failed",
            "payment_intent.canceled",
            "payment_intent.created" -> {
                !processPaymentIntentEvent(event)
            }

            else -> {
                logger.info { "Unhandled event type: ${event.type}" }
                !KIO.fail(Error.InvalidEventType(event.type))
            }
        }

        logger.info { "Webhook processed successfully for payment: ${payment.id}" }

        KIO.ok(
            WebhookResponse(
                received = true,
                paymentId = payment.id.toString()
            )
        )
    }.transact()

    fun getPaymentHistory(userId: Long): App<Error, List<Payment>> = KIO.comprehension {
        PaymentRepository.findByUserId(userId).orDie()
    }

    fun getPaymentByStripeId(stripePaymentIntentId: String): App<Error, Payment> = KIO.comprehension {
        PaymentRepository.findByStripePaymentIntentId(stripePaymentIntentId)
            .orDie()
            .onNullFail { Error.PaymentNotFound }
    }

    private fun processPaymentIntentEvent(event: Event): App<Error, Payment> = KIO.comprehension {
        val data = !StripeService.handlePaymentIntentEvent(event)
            .mapError { Error.StripeError("Failed to parse payment intent") }

        val userIdString = data.metadata["user_id"] ?: !KIO.fail(Error.MissingUserId)

        val userId = userIdString.toLongOrNull()

        !KIO.failOn(userId == null) { Error.InvalidUserId }

        val payment = !PaymentRepository.upsertFromWebhook(
            stripePaymentIntentId = data.id,
            userId = userId!!,
            amount = data.amount,
            currency = data.currency,
            status = data.status,
            metadata = data.metadata
        ).orDie()

        logger.info { "Payment processed: ${payment.id} - Status: ${payment.status}" }
        KIO.ok(payment)
    }
}
