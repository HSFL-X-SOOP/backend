package hs.flensburg.marlin.business.api.notificationMeasurementRule.control

import de.lambda9.tailwind.jooq.JIO
import de.lambda9.tailwind.jooq.Jooq
import hs.flensburg.marlin.business.ApiError
import hs.flensburg.marlin.business.ServiceLayerError
import hs.flensburg.marlin.business.api.notificationMeasurementRule.entity.NotificationMeasurementRuleDTO
import hs.flensburg.marlin.database.generated.tables.pojos.NotificationMeasurementRule
import hs.flensburg.marlin.database.generated.tables.references.NOTIFICATION_MEASUREMENT_RULE


object NotificationMeasurementRuleRepo {
    sealed class Error(private val message: String) : ServiceLayerError {
        object NotFound : Error("Notification measurement rule not found")
        object BadRequest : Error("Bad request")

        override fun toApiError(): ApiError {
            return when (this) {
                is NotFound -> ApiError.NotFound(message)
                is BadRequest -> ApiError.BadRequest(message)
            }
        }
    }

    fun insert(notificationMeasurementRule: NotificationMeasurementRule): JIO<NotificationMeasurementRule> = Jooq.query {
        insertInto(NOTIFICATION_MEASUREMENT_RULE)
            .set(NOTIFICATION_MEASUREMENT_RULE.USER_ID, notificationMeasurementRule.userId)
            .set(NOTIFICATION_MEASUREMENT_RULE.LOCATION_ID, notificationMeasurementRule.locationId)
            .set(NOTIFICATION_MEASUREMENT_RULE.MEASUREMENT_TYPE_ID, notificationMeasurementRule.measurementTypeId)
            .set(NOTIFICATION_MEASUREMENT_RULE.OPERATOR, notificationMeasurementRule.operator)
            .set(NOTIFICATION_MEASUREMENT_RULE.VALU, notificationMeasurementRule.valu)
            .set(NOTIFICATION_MEASUREMENT_RULE.IS_ACTIVE, notificationMeasurementRule.isActive)
            .returning()
            .fetchInto(NotificationMeasurementRule::class.java).first()
    }

    fun update(
        id: Long,
        notificationMeasurementRule: NotificationMeasurementRule
    ): JIO<NotificationMeasurementRule?> = Jooq.query {
        update(NOTIFICATION_MEASUREMENT_RULE)
            .set(NOTIFICATION_MEASUREMENT_RULE.USER_ID, notificationMeasurementRule.userId)
            .set(NOTIFICATION_MEASUREMENT_RULE.LOCATION_ID, notificationMeasurementRule.locationId)
            .set(NOTIFICATION_MEASUREMENT_RULE.MEASUREMENT_TYPE_ID, notificationMeasurementRule.measurementTypeId)
            .set(NOTIFICATION_MEASUREMENT_RULE.OPERATOR, notificationMeasurementRule.operator)
            .set(NOTIFICATION_MEASUREMENT_RULE.VALU, notificationMeasurementRule.valu)
            .set(NOTIFICATION_MEASUREMENT_RULE.IS_ACTIVE, notificationMeasurementRule.isActive)
            .where(NOTIFICATION_MEASUREMENT_RULE.ID.eq(id))
            .returning()
            .fetchOneInto(NotificationMeasurementRule::class.java)
    }

    fun fetchById(id: Long): JIO<NotificationMeasurementRule?> = Jooq.query {
        selectFrom(NOTIFICATION_MEASUREMENT_RULE)
            .where(NOTIFICATION_MEASUREMENT_RULE.ID.eq(id))
            .fetchOneInto(NotificationMeasurementRule::class.java)
    }

    fun fetchAllByUserId(userId: Long): JIO<List<NotificationMeasurementRuleDTO?>> = Jooq.query {
        selectFrom(NOTIFICATION_MEASUREMENT_RULE)
            .where(NOTIFICATION_MEASUREMENT_RULE.USER_ID.eq(userId))
            .fetchInto(NotificationMeasurementRule::class.java)
            .map { NotificationMeasurementRuleDTO.from(it) }
    }

    fun deleteById(id: Long): JIO<Unit> = Jooq.query {
        deleteFrom(NOTIFICATION_MEASUREMENT_RULE)
            .where(NOTIFICATION_MEASUREMENT_RULE.ID.eq(id))
            .execute()
    }

}