package hs.flensburg.marlin.business.api.notificationMeasurementRule.entity

import kotlinx.serialization.Serializable

@Serializable
data class CreateOrUpdateNotificationMeasurementRuleRequest(
    var userId: Long,
    var locationId: Long,
    var measurementTypeId: Long,
    var operator: String,
    var measurementValue: Double,
    var isActive: Boolean
)