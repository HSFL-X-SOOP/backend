package hs.flensburg.marlin.business.api.notificationMeasurementRule.entity

import hs.flensburg.marlin.database.generated.tables.pojos.NotificationMeasurementRule
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toKotlinLocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class NotificationMeasurementRuleDTO(
    var id: Long,
    var userId: Long,
    var locationId: Long,
    var measurementTypeId: Long,
    var operator: String,
    var value: Double,
    var isActive: Boolean,
    var createdAt: LocalDateTime?
) {
    companion object {
        fun from(rule: NotificationMeasurementRule): NotificationMeasurementRuleDTO {
            return NotificationMeasurementRuleDTO(
                id = rule.id!!,
                userId = rule.userId!!,
                locationId = rule.locationId!!,
                measurementTypeId = rule.measurementTypeId!!,
                operator = rule.operator!!,
                value = rule.valu!!,
                isActive = rule.isActive!!,
                createdAt = rule.createdAt?.toKotlinLocalDateTime()

            )
        }
    }
}