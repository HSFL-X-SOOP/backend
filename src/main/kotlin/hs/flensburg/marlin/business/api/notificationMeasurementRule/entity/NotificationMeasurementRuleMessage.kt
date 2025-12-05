package hs.flensburg.marlin.business.api.notificationMeasurementRule.entity

import kotlinx.datetime.LocalDateTime

data class NotificationMeasurementRuleMessage(
    val notificationMeasurementRule: NotificationMeasurementRuleDTO,
    val measurementType: String,
    val measurementUnitSymbol: String?,
    val measurementValue: Double,
    val measurementTime: LocalDateTime?
) {
}