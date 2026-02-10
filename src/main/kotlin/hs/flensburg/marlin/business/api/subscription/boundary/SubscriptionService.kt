package hs.flensburg.marlin.business.api.subscription.boundary

import com.google.gson.JsonParser
import com.stripe.exception.SignatureVerificationException
import com.stripe.model.Event
import com.stripe.model.Subscription
import com.stripe.net.Webhook
import de.lambda9.tailwind.core.KIO
import de.lambda9.tailwind.core.extensions.kio.onNullFail
import de.lambda9.tailwind.core.extensions.kio.orDie
import hs.flensburg.marlin.business.ApiError
import hs.flensburg.marlin.business.App
import hs.flensburg.marlin.business.JEnv
import hs.flensburg.marlin.business.ServiceLayerError
import hs.flensburg.marlin.business.api.subscription.control.StripeSubscriptionService
import hs.flensburg.marlin.business.api.subscription.control.SubscriptionRepository
import hs.flensburg.marlin.business.api.subscription.entity.CancelSubscriptionRequest
import hs.flensburg.marlin.business.api.subscription.entity.CreatePortalSessionRequest
import hs.flensburg.marlin.business.api.subscription.entity.CreateSubscriptionRequest
import hs.flensburg.marlin.business.api.subscription.entity.InvoiceResponse
import hs.flensburg.marlin.business.api.subscription.entity.PauseSubscriptionRequest
import hs.flensburg.marlin.business.api.subscription.entity.PaymentSheetResponse
import hs.flensburg.marlin.business.api.subscription.entity.PortalSessionResponse
import hs.flensburg.marlin.business.api.subscription.entity.ReactivateSubscriptionRequest
import hs.flensburg.marlin.business.api.subscription.entity.ResumeSubscriptionRequest
import hs.flensburg.marlin.business.api.subscription.entity.SetupIntentResponse
import hs.flensburg.marlin.business.api.subscription.entity.SubscriptionStatusResponse
import hs.flensburg.marlin.business.api.subscription.entity.UpdatePaymentMethodRequest
import hs.flensburg.marlin.business.api.subscription.entity.UserSubscriptionsResponse
import hs.flensburg.marlin.database.generated.enums.SubscriptionStatus
import hs.flensburg.marlin.database.generated.enums.SubscriptionType
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset

object SubscriptionService {

    sealed class Error(private val message: String) : ServiceLayerError {
        object AlreadySubscribed : Error("User already has an active subscription of this type")
        object NotFound : Error("Subscription not found")
        object InvalidSignature : Error("Invalid webhook signature")
        object CustomerNotFound : Error("No Stripe customer found for this user")
        object MissingClientSecret : Error("No payment intent or setup intent client secret returned")
        object NotPendingCancellation : Error("Subscription is not pending cancellation")
        object InvoiceNotFound : Error("Invoice not found")
        object NoPdfAvailable : Error("No PDF available for this invoice")
        object NotPaused : Error("Subscription is not paused")
        object AlreadyPaused : Error("Subscription is already paused")
        data class StripeError(val detail: String) : Error("Stripe error: $detail")
        data class InvalidEvent(val detail: String) : Error("Invalid webhook event: $detail")

        override fun toApiError(): ApiError {
            return when (this) {
                is AlreadySubscribed -> ApiError.Conflict(message)
                is NotFound -> ApiError.NotFound(message)
                is InvalidSignature -> ApiError.Unauthorized(message)
                is CustomerNotFound -> ApiError.NotFound(message)
                is MissingClientSecret -> ApiError.BadGateway(message)
                is NotPendingCancellation -> ApiError.Conflict(message)
                is InvoiceNotFound -> ApiError.NotFound(message)
                is NoPdfAvailable -> ApiError.NotFound(message)
                is NotPaused -> ApiError.Conflict(message)
                is AlreadyPaused -> ApiError.Conflict(message)
                is StripeError -> ApiError.BadGateway(message)
                is InvalidEvent -> ApiError.BadRequest(message)
            }
        }
    }

    private val logger = KotlinLogging.logger {}

    fun createSubscription(
        userId: Long,
        email: String,
        request: CreateSubscriptionRequest
    ): App<ServiceLayerError, PaymentSheetResponse> = KIO.comprehension {
        val (_, env) = !KIO.access<JEnv>()

        val alreadyActive = !SubscriptionRepository.hasActiveSubscription(userId, request.subscriptionType).orDie()
        !KIO.failOn(alreadyActive) { Error.AlreadySubscribed }

        val customerId = !StripeSubscriptionService.getOrCreateCustomer(userId, email)

        val hadBefore = !SubscriptionRepository.hasEverHadSubscription(userId, request.subscriptionType).orDie()
        val trialDays = if (hadBefore) null else env.config.stripe.trialDays

        val priceId = when (request.subscriptionType) {
            SubscriptionType.APP_NOTIFICATION -> env.config.stripe.notificationPriceId
            SubscriptionType.API_ACCESS -> env.config.stripe.apiAccessPriceId
        }

        val ephemeralKey = !StripeSubscriptionService.createEphemeralKey(customerId)

        val subscription = !StripeSubscriptionService.createSubscription(
            customerId = customerId,
            priceId = priceId,
            subscriptionType = request.subscriptionType,
            userId = userId,
            trialDays = trialDays
        )

        val subscriptionJson = JsonParser.parseString(subscription.toJson()).asJsonObject

        val paymentIntentClientSecret = subscriptionJson
            .get("latest_invoice")?.takeIf { !it.isJsonNull }?.asJsonObject
            ?.get("payment_intent")?.takeIf { !it.isJsonNull }?.asJsonObject
            ?.get("client_secret")?.takeIf { !it.isJsonNull }?.asString

        val setupIntentClientSecret = subscriptionJson
            .get("pending_setup_intent")?.takeIf { !it.isJsonNull }?.asJsonObject
            ?.get("client_secret")?.takeIf { !it.isJsonNull }?.asString

        if (paymentIntentClientSecret == null && setupIntentClientSecret == null) {
            !KIO.fail(Error.MissingClientSecret)
        }

        KIO.ok(
            PaymentSheetResponse(
                paymentIntent = paymentIntentClientSecret,
                setupIntent = setupIntentClientSecret,
                ephemeralKey = ephemeralKey,
                customerId = customerId,
                publishableKey = env.config.stripe.publishableKey
            )
        )
    }

    fun createPortalSession(
        userId: Long,
        request: CreatePortalSessionRequest
    ): App<ServiceLayerError, PortalSessionResponse> = KIO.comprehension {
        val user = !hs.flensburg.marlin.business.api.users.control.UserRepo.fetchById(userId).orDie()
            .onNullFail { Error.CustomerNotFound }

        val customerId = user.stripeCustomerId

        !KIO.failOn(customerId == null) { Error.CustomerNotFound }

        val session = !StripeSubscriptionService.createPortalSession(customerId!!, request.returnUrl)

        KIO.ok(PortalSessionResponse(url = session.url))
    }

    fun getSubscriptionStatus(userId: Long): App<ServiceLayerError, UserSubscriptionsResponse> = KIO.comprehension {
        val notificationSub =
            !SubscriptionRepository.findActiveByUserIdAndType(userId, SubscriptionType.APP_NOTIFICATION).orDie()
        val apiSub = !SubscriptionRepository.findActiveByUserIdAndType(userId, SubscriptionType.API_ACCESS).orDie()

        KIO.ok(
            UserSubscriptionsResponse(
                notifications = SubscriptionStatusResponse(
                    subscriptionType = SubscriptionType.APP_NOTIFICATION,
                    status = notificationSub?.status,
                    currentPeriodEnd = notificationSub?.currentPeriodEnd?.toString(),
                    cancelAtPeriodEnd = notificationSub?.cancelAtPeriodEnd ?: false,
                    trialEnd = notificationSub?.trialEnd?.toString()
                ),
                apiAccess = SubscriptionStatusResponse(
                    subscriptionType = SubscriptionType.API_ACCESS,
                    status = apiSub?.status,
                    currentPeriodEnd = apiSub?.currentPeriodEnd?.toString(),
                    cancelAtPeriodEnd = apiSub?.cancelAtPeriodEnd ?: false,
                    trialEnd = apiSub?.trialEnd?.toString()
                )
            )
        )
    }

    fun cancelSubscription(
        userId: Long,
        request: CancelSubscriptionRequest
    ): App<ServiceLayerError, SubscriptionStatusResponse> = KIO.comprehension {
        val sub = !SubscriptionRepository.findActiveByUserIdAndType(userId, request.subscriptionType).orDie()
            .onNullFail { Error.NotFound }

        !StripeSubscriptionService.cancelSubscriptionAtPeriodEnd(sub.stripeSubscriptionId!!)

        val updated = !SubscriptionRepository.findByStripeSubscriptionId(sub.stripeSubscriptionId!!).orDie()
            .onNullFail { Error.NotFound }

        KIO.ok(
            SubscriptionStatusResponse(
                subscriptionType = request.subscriptionType,
                status = updated.status,
                currentPeriodEnd = updated.currentPeriodEnd?.toString(),
                cancelAtPeriodEnd = true,
                trialEnd = updated.trialEnd?.toString()
            )
        )
    }

    fun reactivateSubscription(
        userId: Long,
        request: ReactivateSubscriptionRequest
    ): App<ServiceLayerError, SubscriptionStatusResponse> = KIO.comprehension {
        val sub = !SubscriptionRepository.findActiveByUserIdAndType(userId, request.subscriptionType).orDie()
            .onNullFail { Error.NotFound }

        !KIO.failOn(sub.cancelAtPeriodEnd != true) { Error.NotPendingCancellation }

        !StripeSubscriptionService.reactivateSubscription(sub.stripeSubscriptionId!!)

        val updated = !SubscriptionRepository.findByStripeSubscriptionId(sub.stripeSubscriptionId!!).orDie()
            .onNullFail { Error.NotFound }

        KIO.ok(
            SubscriptionStatusResponse(
                subscriptionType = request.subscriptionType,
                status = updated.status,
                currentPeriodEnd = updated.currentPeriodEnd?.toString(),
                cancelAtPeriodEnd = false,
                trialEnd = updated.trialEnd?.toString()
            )
        )
    }

    fun listInvoices(userId: Long): App<ServiceLayerError, List<InvoiceResponse>> = KIO.comprehension {
        val user = !hs.flensburg.marlin.business.api.users.control.UserRepo.fetchById(userId).orDie()
            .onNullFail { Error.CustomerNotFound }

        val customerId = user.stripeCustomerId
        !KIO.failOn(customerId == null) { Error.CustomerNotFound }

        val invoices = !StripeSubscriptionService.listInvoices(customerId!!)

        KIO.ok(invoices.map { invoice ->
            InvoiceResponse(
                id = invoice.id,
                amountDue = invoice.amountDue,
                amountPaid = invoice.amountPaid,
                currency = invoice.currency,
                status = invoice.status,
                invoicePdf = invoice.invoicePdf,
                created = invoice.created,
                periodStart = invoice.periodStart,
                periodEnd = invoice.periodEnd
            )
        })
    }

    fun getInvoicePdf(userId: Long, invoiceId: String): App<ServiceLayerError, String> = KIO.comprehension {
        val user = !hs.flensburg.marlin.business.api.users.control.UserRepo.fetchById(userId).orDie()
            .onNullFail { Error.CustomerNotFound }

        val customerId = user.stripeCustomerId
        !KIO.failOn(customerId == null) { Error.CustomerNotFound }

        val invoice = !StripeSubscriptionService.getInvoice(invoiceId)

        !KIO.failOn(invoice.customer != customerId) { Error.InvoiceNotFound }
        !KIO.failOn(invoice.invoicePdf == null) { Error.NoPdfAvailable }

        KIO.ok(invoice.invoicePdf)
    }

    fun createSetupIntent(userId: Long): App<ServiceLayerError, SetupIntentResponse> = KIO.comprehension {
        val user = !hs.flensburg.marlin.business.api.users.control.UserRepo.fetchById(userId).orDie()
            .onNullFail { Error.CustomerNotFound }

        val customerId = user.stripeCustomerId
        !KIO.failOn(customerId == null) { Error.CustomerNotFound }

        val setupIntent = !StripeSubscriptionService.createSetupIntent(customerId!!)

        KIO.ok(SetupIntentResponse(clientSecret = setupIntent.clientSecret))
    }

    fun updatePaymentMethod(
        userId: Long,
        request: UpdatePaymentMethodRequest
    ): App<ServiceLayerError, Map<String, Boolean>> = KIO.comprehension {
        val user = !hs.flensburg.marlin.business.api.users.control.UserRepo.fetchById(userId).orDie()
            .onNullFail { Error.CustomerNotFound }

        val customerId = user.stripeCustomerId
        !KIO.failOn(customerId == null) { Error.CustomerNotFound }

        !StripeSubscriptionService.updateDefaultPaymentMethod(customerId!!, request.paymentMethodId)

        KIO.ok(mapOf("updated" to true))
    }

    fun pauseSubscription(
        userId: Long,
        request: PauseSubscriptionRequest
    ): App<ServiceLayerError, SubscriptionStatusResponse> = KIO.comprehension {
        val sub = !SubscriptionRepository.findActiveByUserIdAndType(userId, request.subscriptionType).orDie()
            .onNullFail { Error.NotFound }

        !StripeSubscriptionService.pauseSubscription(sub.stripeSubscriptionId!!)

        val updated = !SubscriptionRepository.findByStripeSubscriptionId(sub.stripeSubscriptionId!!).orDie()
            .onNullFail { Error.NotFound }

        KIO.ok(
            SubscriptionStatusResponse(
                subscriptionType = request.subscriptionType,
                status = updated.status,
                currentPeriodEnd = updated.currentPeriodEnd?.toString(),
                cancelAtPeriodEnd = updated.cancelAtPeriodEnd ?: false,
                trialEnd = updated.trialEnd?.toString()
            )
        )
    }

    fun resumeSubscription(
        userId: Long,
        request: ResumeSubscriptionRequest
    ): App<ServiceLayerError, SubscriptionStatusResponse> = KIO.comprehension {
        val sub = !SubscriptionRepository.findActiveByUserIdAndType(userId, request.subscriptionType).orDie()
            .onNullFail { Error.NotFound }

        !StripeSubscriptionService.resumeSubscription(sub.stripeSubscriptionId!!)

        val updated = !SubscriptionRepository.findByStripeSubscriptionId(sub.stripeSubscriptionId!!).orDie()
            .onNullFail { Error.NotFound }

        KIO.ok(
            SubscriptionStatusResponse(
                subscriptionType = request.subscriptionType,
                status = updated.status,
                currentPeriodEnd = updated.currentPeriodEnd?.toString(),
                cancelAtPeriodEnd = updated.cancelAtPeriodEnd ?: false,
                trialEnd = updated.trialEnd?.toString()
            )
        )
    }

    fun processWebhook(
        payload: String,
        signature: String
    ): App<ServiceLayerError, Map<String, Boolean>> = KIO.comprehension {
        val (_, env) = !KIO.access<JEnv>()

        val event = try {
            Webhook.constructEvent(
                payload,
                signature,
                env.config.stripe.webhookSecret
            )
        } catch (e: SignatureVerificationException) {
            logger.error(e) { "Invalid subscription webhook signature" }
            !KIO.fail(Error.InvalidSignature)
        } catch (e: Exception) {
            logger.error(e) { "Error verifying subscription webhook: ${e.message}" }
            !KIO.fail(Error.StripeError(e.message ?: "Unknown error"))
        }

        when (event.type) {
            "customer.subscription.created",
            "customer.subscription.updated",
            "customer.subscription.deleted" -> {
                !handleSubscriptionEvent(event)
            }

            "invoice.payment_failed" -> {
                logger.warn { "Invoice payment failed: ${event.id}" }
            }

            else -> {
                logger.info { "Ignoring unhandled event type: ${event.type}" }
            }
        }

        KIO.ok(mapOf("received" to true))
    }

    private fun handleSubscriptionEvent(event: Event): App<ServiceLayerError, Unit> = KIO.comprehension {
        val stripeSubscription = event.dataObjectDeserializer.`object`.orElse(null) as? Subscription
            ?: !KIO.fail(Error.InvalidEvent("Could not deserialize subscription from event"))

        val metadata = stripeSubscription.metadata ?: emptyMap()
        val userId = metadata["user_id"]?.toLongOrNull()
            ?: !KIO.fail(Error.InvalidEvent("Missing user_id in subscription metadata"))

        val subscriptionTypeStr = metadata["subscription_type"]
            ?: !KIO.fail(Error.InvalidEvent("Missing subscription_type in subscription metadata"))

        val subscriptionType = try {
            SubscriptionType.valueOf(subscriptionTypeStr)
        } catch (_: IllegalArgumentException) {
            logger.error { "Invalid subscription type in metadata: $subscriptionTypeStr" }
            !KIO.fail(Error.InvalidEvent("Invalid subscription_type: $subscriptionTypeStr"))
        }

        val status = mapStripeStatus(stripeSubscription.status)

        val priceId = stripeSubscription.items?.data?.firstOrNull()?.price?.id ?: ""

        val subJson = JsonParser.parseString(stripeSubscription.toJson()).asJsonObject
        val periodStart = subJson.get("current_period_start")?.takeIf { !it.isJsonNull }?.asLong
        val periodEnd = subJson.get("current_period_end")?.takeIf { !it.isJsonNull }?.asLong

        !SubscriptionRepository.upsertFromWebhook(
            stripeSubscriptionId = stripeSubscription.id,
            userId = userId,
            stripeCustomerId = stripeSubscription.customer,
            stripePriceId = priceId,
            subscriptionType = subscriptionType,
            status = status,
            currentPeriodStart = epochToOffsetDateTime(periodStart),
            currentPeriodEnd = epochToOffsetDateTime(periodEnd),
            cancelAtPeriodEnd = stripeSubscription.cancelAtPeriodEnd ?: false,
            trialStart = epochToOffsetDateTime(stripeSubscription.trialStart),
            trialEnd = epochToOffsetDateTime(stripeSubscription.trialEnd),
            canceledAt = epochToOffsetDateTime(stripeSubscription.canceledAt)
        ).orDie()

        logger.info { "Processed subscription event ${event.type} for subscription ${stripeSubscription.id}" }
        KIO.ok(Unit)
    }

    private fun mapStripeStatus(stripeStatus: String): SubscriptionStatus {
        return when (stripeStatus) {
            "active" -> SubscriptionStatus.ACTIVE
            "trialing" -> SubscriptionStatus.TRIALING
            "past_due" -> SubscriptionStatus.PAST_DUE
            "canceled" -> SubscriptionStatus.CANCELED
            "unpaid" -> SubscriptionStatus.UNPAID
            "incomplete" -> SubscriptionStatus.INCOMPLETE
            "incomplete_expired" -> SubscriptionStatus.INCOMPLETE_EXPIRED
            "paused" -> SubscriptionStatus.PAUSED
            else -> SubscriptionStatus.CANCELED
        }
    }

    private fun epochToOffsetDateTime(epoch: Long?): OffsetDateTime? {
        return epoch?.let { OffsetDateTime.ofInstant(Instant.ofEpochSecond(it), ZoneOffset.UTC) }
    }
}
