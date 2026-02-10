package hs.flensburg.marlin.business.api.subscription.control

import de.lambda9.tailwind.jooq.JIO
import de.lambda9.tailwind.jooq.Jooq
import hs.flensburg.marlin.database.generated.enums.SubscriptionStatus
import hs.flensburg.marlin.database.generated.enums.SubscriptionType
import hs.flensburg.marlin.database.generated.tables.pojos.Subscription
import hs.flensburg.marlin.database.generated.tables.references.SUBSCRIPTION
import java.time.OffsetDateTime

object SubscriptionRepository {
    fun findByStripeSubscriptionId(stripeSubscriptionId: String): JIO<Subscription?> = Jooq.query {
        selectFrom(SUBSCRIPTION)
            .where(SUBSCRIPTION.STRIPE_SUBSCRIPTION_ID.eq(stripeSubscriptionId))
            .fetchOneInto(Subscription::class.java)
    }

    fun findActiveByUserIdAndType(userId: Long, type: SubscriptionType): JIO<Subscription?> = Jooq.query {
        selectFrom(SUBSCRIPTION)
            .where(
                SUBSCRIPTION.USER_ID.eq(userId)
                    .and(SUBSCRIPTION.SUBSCRIPTION_TYPE.eq(type))
                    .and(SUBSCRIPTION.STATUS.`in`(SubscriptionStatus.ACTIVE, SubscriptionStatus.TRIALING, SubscriptionStatus.PAST_DUE))
            )
            .fetchOneInto(Subscription::class.java)
    }

    fun hasActiveSubscription(userId: Long, type: SubscriptionType): JIO<Boolean> = Jooq.query {
        fetchExists(
            selectFrom(SUBSCRIPTION)
                .where(
                    SUBSCRIPTION.USER_ID.eq(userId)
                        .and(SUBSCRIPTION.SUBSCRIPTION_TYPE.eq(type))
                        .and(SUBSCRIPTION.STATUS.`in`(SubscriptionStatus.ACTIVE, SubscriptionStatus.TRIALING))
                )
        )
    }

    fun hasEverHadSubscription(userId: Long, type: SubscriptionType): JIO<Boolean> = Jooq.query {
        fetchExists(
            selectFrom(SUBSCRIPTION)
                .where(
                    SUBSCRIPTION.USER_ID.eq(userId)
                        .and(SUBSCRIPTION.SUBSCRIPTION_TYPE.eq(type))
                )
        )
    }

    fun upsertFromWebhook(
        stripeSubscriptionId: String,
        userId: Long,
        stripeCustomerId: String,
        stripePriceId: String,
        subscriptionType: SubscriptionType,
        status: SubscriptionStatus,
        currentPeriodStart: OffsetDateTime?,
        currentPeriodEnd: OffsetDateTime?,
        cancelAtPeriodEnd: Boolean,
        trialStart: OffsetDateTime?,
        trialEnd: OffsetDateTime?,
        canceledAt: OffsetDateTime?
    ): JIO<Subscription> = Jooq.query {
        val existing = selectFrom(SUBSCRIPTION)
            .where(SUBSCRIPTION.STRIPE_SUBSCRIPTION_ID.eq(stripeSubscriptionId))
            .fetchOneInto(Subscription::class.java)

        if (existing != null) {
            update(SUBSCRIPTION)
                .set(SUBSCRIPTION.STATUS, status)
                .set(SUBSCRIPTION.STRIPE_PRICE_ID, stripePriceId)
                .set(SUBSCRIPTION.CURRENT_PERIOD_START, currentPeriodStart)
                .set(SUBSCRIPTION.CURRENT_PERIOD_END, currentPeriodEnd)
                .set(SUBSCRIPTION.CANCEL_AT_PERIOD_END, cancelAtPeriodEnd)
                .set(SUBSCRIPTION.TRIAL_START, trialStart)
                .set(SUBSCRIPTION.TRIAL_END, trialEnd)
                .set(SUBSCRIPTION.CANCELED_AT, canceledAt)
                .where(SUBSCRIPTION.STRIPE_SUBSCRIPTION_ID.eq(stripeSubscriptionId))
                .returning()
                .fetchOneInto(Subscription::class.java)!!
        } else {
            insertInto(SUBSCRIPTION)
                .set(SUBSCRIPTION.USER_ID, userId)
                .set(SUBSCRIPTION.STRIPE_CUSTOMER_ID, stripeCustomerId)
                .set(SUBSCRIPTION.STRIPE_SUBSCRIPTION_ID, stripeSubscriptionId)
                .set(SUBSCRIPTION.STRIPE_PRICE_ID, stripePriceId)
                .set(SUBSCRIPTION.SUBSCRIPTION_TYPE, subscriptionType)
                .set(SUBSCRIPTION.STATUS, status)
                .set(SUBSCRIPTION.CURRENT_PERIOD_START, currentPeriodStart)
                .set(SUBSCRIPTION.CURRENT_PERIOD_END, currentPeriodEnd)
                .set(SUBSCRIPTION.CANCEL_AT_PERIOD_END, cancelAtPeriodEnd)
                .set(SUBSCRIPTION.TRIAL_START, trialStart)
                .set(SUBSCRIPTION.TRIAL_END, trialEnd)
                .set(SUBSCRIPTION.CANCELED_AT, canceledAt)
                .returning()
                .fetchOneInto(Subscription::class.java)!!
        }
    }
}
