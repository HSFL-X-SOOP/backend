package hs.flensburg.marlin.business.api.payment.control

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import de.lambda9.tailwind.jooq.JIO
import de.lambda9.tailwind.jooq.Jooq
import hs.flensburg.marlin.database.generated.enums.PaymentStatus
import hs.flensburg.marlin.database.generated.tables.pojos.Payment
import hs.flensburg.marlin.database.generated.tables.records.PaymentRecord
import hs.flensburg.marlin.database.generated.tables.references.PAYMENT
import org.jooq.JSONB
import java.util.*

object PaymentRepository {
    private val objectMapper = jacksonObjectMapper()

    fun findByStripePaymentIntentId(stripePaymentIntentId: String): JIO<Payment?> = Jooq.query {
        selectFrom(PAYMENT)
            .where(PAYMENT.STRIPE_PAYMENT_INTENT_ID.eq(stripePaymentIntentId))
            .fetchOneInto(Payment::class.java)
    }

    fun findById(id: UUID): JIO<Payment?> = Jooq.query {
        selectFrom(PAYMENT)
            .where(PAYMENT.ID.eq(id))
            .fetchOneInto(Payment::class.java)
    }

    fun updateStatus(
        stripePaymentIntentId: String,
        status: PaymentStatus
    ): JIO<Payment?> = Jooq.query {
        update(PAYMENT)
            .set(PAYMENT.STATUS, status)
            .set(PAYMENT.UPDATED_AT, org.jooq.impl.DSL.currentOffsetDateTime())
            .where(PAYMENT.STRIPE_PAYMENT_INTENT_ID.eq(stripePaymentIntentId))
            .returning()
            .fetchOneInto(Payment::class.java)
    }

    fun updatePayment(
        stripePaymentIntentId: String,
        status: PaymentStatus,
        metadata: Map<String, String>? = null
    ): JIO<Payment?> = Jooq.query {
        var updateQuery = update(PAYMENT)
            .set(PAYMENT.STATUS, status)
            .set(PAYMENT.UPDATED_AT, org.jooq.impl.DSL.currentOffsetDateTime())

        if (metadata != null) {
            updateQuery = updateQuery.set(PAYMENT.METADATA, JSONB.valueOf(objectMapper.writeValueAsString(metadata)))
        }

        updateQuery
            .where(PAYMENT.STRIPE_PAYMENT_INTENT_ID.eq(stripePaymentIntentId))
            .returning()
            .fetchOneInto(Payment::class.java)
    }

    fun findByUserId(userId: Long): JIO<List<Payment>> = Jooq.query {
        selectFrom(PAYMENT)
            .where(PAYMENT.USER_ID.eq(userId))
            .orderBy(PAYMENT.CREATED_AT.desc())
            .fetchInto(Payment::class.java)
    }

    fun findSuccessfulPaymentsByUserId(userId: Long): JIO<List<Payment>> = Jooq.query {
        selectFrom(PAYMENT)
            .where(PAYMENT.USER_ID.eq(userId))
            .and(PAYMENT.STATUS.eq(PaymentStatus.SUCCEEDED))
            .orderBy(PAYMENT.CREATED_AT.desc())
            .fetchInto(Payment::class.java)
    }

    fun upsertFromWebhook(
        stripePaymentIntentId: String,
        userId: Long,
        amount: Long,
        currency: String,
        status: PaymentStatus,
        metadata: Map<String, String>?
    ): JIO<Payment> = Jooq.query {
        val existing = selectFrom(PAYMENT)
            .where(PAYMENT.STRIPE_PAYMENT_INTENT_ID.eq(stripePaymentIntentId))
            .fetchOneInto(Payment::class.java)

        if (existing != null) {
            var updateQuery = update(PAYMENT)
                .set(PAYMENT.STATUS, status)

            if (metadata != null) {
                updateQuery =
                    updateQuery.set(PAYMENT.METADATA, JSONB.valueOf(objectMapper.writeValueAsString(metadata)))
            }

            updateQuery
                .where(PAYMENT.STRIPE_PAYMENT_INTENT_ID.eq(stripePaymentIntentId))
                .returning()
                .fetchOneInto(Payment::class.java)!!
        } else {
            val record = PaymentRecord().apply {
                this.id = UUID.randomUUID()
                this.userId = userId
                this.stripePaymentIntentId = stripePaymentIntentId
                this.amount = amount
                this.currency = currency
                this.status = status
                if (metadata != null) {
                    this.metadata = JSONB.valueOf(objectMapper.writeValueAsString(metadata))
                }
            }

            insertInto(PAYMENT)
                .set(record)
                .returning()
                .fetchOneInto(Payment::class.java)!!
        }
    }
}
