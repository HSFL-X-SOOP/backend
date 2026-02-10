package hs.flensburg.marlin.business.api.subscription.control

import com.stripe.Stripe
import com.stripe.exception.StripeException
import com.stripe.model.Customer
import com.stripe.model.EphemeralKey
import com.stripe.model.Invoice
import com.stripe.model.SetupIntent
import com.stripe.model.Subscription
import com.stripe.model.billingportal.Session as PortalSession
import com.stripe.param.CustomerCreateParams
import com.stripe.param.CustomerUpdateParams
import com.stripe.param.EphemeralKeyCreateParams
import com.stripe.param.InvoiceListParams
import com.stripe.param.SetupIntentCreateParams
import com.stripe.param.SubscriptionCreateParams
import com.stripe.param.SubscriptionUpdateParams
import com.stripe.param.common.EmptyParam
import de.lambda9.tailwind.core.KIO
import de.lambda9.tailwind.core.extensions.kio.orDie
import hs.flensburg.marlin.business.ApiError
import hs.flensburg.marlin.business.App
import hs.flensburg.marlin.business.ServiceLayerError
import hs.flensburg.marlin.business.api.users.control.UserRepo
import hs.flensburg.marlin.database.generated.enums.SubscriptionType
import io.github.oshai.kotlinlogging.KotlinLogging

object StripeSubscriptionService {

    sealed class Error(private val message: String) : ServiceLayerError {
        data class StripeApiError(val detail: String) : Error("Stripe API error: $detail")
        object CustomerNotFound : Error("Stripe customer not found")

        override fun toApiError(): ApiError {
            return when (this) {
                is StripeApiError -> ApiError.BadGateway(message)
                is CustomerNotFound -> ApiError.NotFound(message)
            }
        }
    }

    private val logger = KotlinLogging.logger {}

    fun getOrCreateCustomer(userId: Long, email: String): App<Error, String> = KIO.comprehension {
        val user = !UserRepo.fetchById(userId).orDie()
        val existingCustomerId = user?.stripeCustomerId

        if (existingCustomerId != null) {
            KIO.ok(existingCustomerId)
        } else {
            try {
                val params = CustomerCreateParams.builder()
                    .setEmail(email)
                    .putMetadata("user_id", userId.toString())
                    .build()

                val customer = Customer.create(params)
                logger.info { "Created Stripe customer ${customer.id} for user $userId" }

                !UserRepo.setStripeCustomerId(userId, customer.id).orDie()

                KIO.ok(customer.id)
            } catch (e: StripeException) {
                logger.error(e) { "Stripe API error creating customer: ${e.message}" }
                KIO.fail(Error.StripeApiError(e.message ?: "Unknown Stripe error"))
            }
        }
    }

    fun createEphemeralKey(customerId: String): App<Error, String> = KIO.comprehension {
        try {
            val params = EphemeralKeyCreateParams.builder()
                .setCustomer(customerId)
                .setStripeVersion(Stripe.API_VERSION)
                .build()

            val ephemeralKey = EphemeralKey.create(params)
            KIO.ok(ephemeralKey.secret)
        } catch (e: StripeException) {
            logger.error(e) { "Stripe API error creating ephemeral key: ${e.message}" }
            KIO.fail(Error.StripeApiError(e.message ?: "Unknown Stripe error"))
        }
    }

    fun createSubscription(
        customerId: String,
        priceId: String,
        subscriptionType: SubscriptionType,
        userId: Long,
        trialDays: Int?
    ): App<Error, Subscription> = KIO.comprehension {
        try {
            val builder = SubscriptionCreateParams.builder()
                .setCustomer(customerId)
                .addItem(
                    SubscriptionCreateParams.Item.builder()
                        .setPrice(priceId)
                        .build()
                )
                .setPaymentBehavior(SubscriptionCreateParams.PaymentBehavior.DEFAULT_INCOMPLETE)
                .setPaymentSettings(
                    SubscriptionCreateParams.PaymentSettings.builder()
                        .setSaveDefaultPaymentMethod(
                            SubscriptionCreateParams.PaymentSettings.SaveDefaultPaymentMethod.ON_SUBSCRIPTION
                        )
                        .build()
                )
                .addExpand("latest_invoice.payment_intent")
                .addExpand("pending_setup_intent")
                .putMetadata("user_id", userId.toString())
                .putMetadata("subscription_type", subscriptionType.literal)

            if (trialDays != null && trialDays > 0) {
                builder.setTrialPeriodDays(trialDays.toLong())
            }

            val subscription = Subscription.create(builder.build())
            logger.info { "Created subscription ${subscription.id} for customer $customerId" }
            KIO.ok(subscription)
        } catch (e: StripeException) {
            logger.error(e) { "Stripe API error creating subscription: ${e.message}" }
            KIO.fail(Error.StripeApiError(e.message ?: "Unknown Stripe error"))
        }
    }

    fun createPortalSession(
        customerId: String,
        returnUrl: String
    ): App<Error, PortalSession> = KIO.comprehension {
        try {
            val params = com.stripe.param.billingportal.SessionCreateParams.builder()
                .setCustomer(customerId)
                .setReturnUrl(returnUrl)
                .build()

            val session = PortalSession.create(params)
            logger.info { "Created portal session for customer $customerId" }
            KIO.ok(session)
        } catch (e: StripeException) {
            logger.error(e) { "Stripe API error creating portal session: ${e.message}" }
            KIO.fail(Error.StripeApiError(e.message ?: "Unknown Stripe error"))
        }
    }

    fun cancelSubscriptionAtPeriodEnd(stripeSubscriptionId: String): App<Error, Subscription> = KIO.comprehension {
        try {
            val subscription = Subscription.retrieve(stripeSubscriptionId)
            val params = SubscriptionUpdateParams.builder()
                .setCancelAtPeriodEnd(true)
                .build()

            val updated = subscription.update(params)
            logger.info { "Marked subscription $stripeSubscriptionId for cancellation at period end" }
            KIO.ok(updated)
        } catch (e: StripeException) {
            logger.error(e) { "Stripe API error canceling subscription: ${e.message}" }
            KIO.fail(Error.StripeApiError(e.message ?: "Unknown Stripe error"))
        }
    }

    fun reactivateSubscription(stripeSubscriptionId: String): App<Error, Subscription> = KIO.comprehension {
        try {
            val subscription = Subscription.retrieve(stripeSubscriptionId)
            val params = SubscriptionUpdateParams.builder()
                .setCancelAtPeriodEnd(false)
                .build()

            val updated = subscription.update(params)
            logger.info { "Reactivated subscription $stripeSubscriptionId" }
            KIO.ok(updated)
        } catch (e: StripeException) {
            logger.error(e) { "Stripe API error reactivating subscription: ${e.message}" }
            KIO.fail(Error.StripeApiError(e.message ?: "Unknown Stripe error"))
        }
    }

    fun listInvoices(customerId: String): App<Error, List<Invoice>> = KIO.comprehension {
        try {
            val params = InvoiceListParams.builder()
                .setCustomer(customerId)
                .setLimit(100)
                .build()

            val invoices = Invoice.list(params)
            KIO.ok(invoices.data)
        } catch (e: StripeException) {
            logger.error(e) { "Stripe API error listing invoices: ${e.message}" }
            KIO.fail(Error.StripeApiError(e.message ?: "Unknown Stripe error"))
        }
    }

    fun getInvoice(invoiceId: String): App<Error, Invoice> = KIO.comprehension {
        try {
            val invoice = Invoice.retrieve(invoiceId)
            KIO.ok(invoice)
        } catch (e: StripeException) {
            logger.error(e) { "Stripe API error retrieving invoice: ${e.message}" }
            KIO.fail(Error.StripeApiError(e.message ?: "Unknown Stripe error"))
        }
    }

    fun createSetupIntent(customerId: String): App<Error, SetupIntent> = KIO.comprehension {
        try {
            val params = SetupIntentCreateParams.builder()
                .setCustomer(customerId)
                .addPaymentMethodType("card")
                .build()

            val setupIntent = SetupIntent.create(params)
            logger.info { "Created SetupIntent for customer $customerId" }
            KIO.ok(setupIntent)
        } catch (e: StripeException) {
            logger.error(e) { "Stripe API error creating SetupIntent: ${e.message}" }
            KIO.fail(Error.StripeApiError(e.message ?: "Unknown Stripe error"))
        }
    }

    fun updateDefaultPaymentMethod(customerId: String, paymentMethodId: String): App<Error, Customer> = KIO.comprehension {
        try {
            val customer = Customer.retrieve(customerId)
            val params = CustomerUpdateParams.builder()
                .setInvoiceSettings(
                    CustomerUpdateParams.InvoiceSettings.builder()
                        .setDefaultPaymentMethod(paymentMethodId)
                        .build()
                )
                .build()

            val updated = customer.update(params)
            logger.info { "Updated default payment method for customer $customerId" }
            KIO.ok(updated)
        } catch (e: StripeException) {
            logger.error(e) { "Stripe API error updating payment method: ${e.message}" }
            KIO.fail(Error.StripeApiError(e.message ?: "Unknown Stripe error"))
        }
    }

    fun pauseSubscription(stripeSubscriptionId: String): App<Error, Subscription> = KIO.comprehension {
        try {
            val subscription = Subscription.retrieve(stripeSubscriptionId)
            val params = SubscriptionUpdateParams.builder()
                .setPauseCollection(
                    SubscriptionUpdateParams.PauseCollection.builder()
                        .setBehavior(SubscriptionUpdateParams.PauseCollection.Behavior.VOID)
                        .build()
                )
                .build()

            val updated = subscription.update(params)
            logger.info { "Paused subscription $stripeSubscriptionId" }
            KIO.ok(updated)
        } catch (e: StripeException) {
            logger.error(e) { "Stripe API error pausing subscription: ${e.message}" }
            KIO.fail(Error.StripeApiError(e.message ?: "Unknown Stripe error"))
        }
    }

    fun resumeSubscription(stripeSubscriptionId: String): App<Error, Subscription> = KIO.comprehension {
        try {
            val subscription = Subscription.retrieve(stripeSubscriptionId)
            val params = SubscriptionUpdateParams.builder()
                .setPauseCollection(EmptyParam.EMPTY)
                .build()

            val updated = subscription.update(params)
            logger.info { "Resumed subscription $stripeSubscriptionId" }
            KIO.ok(updated)
        } catch (e: StripeException) {
            logger.error(e) { "Stripe API error resuming subscription: ${e.message}" }
            KIO.fail(Error.StripeApiError(e.message ?: "Unknown Stripe error"))
        }
    }
}
