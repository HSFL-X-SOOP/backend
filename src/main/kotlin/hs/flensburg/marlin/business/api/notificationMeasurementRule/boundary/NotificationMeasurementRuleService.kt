package hs.flensburg.marlin.business.api.notificationMeasurementRule.boundary

import de.lambda9.tailwind.core.KIO
import de.lambda9.tailwind.core.extensions.kio.onNullFail
import de.lambda9.tailwind.core.extensions.kio.orDie
import hs.flensburg.marlin.business.ApiError
import hs.flensburg.marlin.business.App
import hs.flensburg.marlin.business.JEnv
import hs.flensburg.marlin.business.ServiceLayerError
import hs.flensburg.marlin.business.api.notificationMeasurementRule.control.NotificationMeasurementRuleRepo
import hs.flensburg.marlin.business.api.notificationMeasurementRule.entity.CreateOrUpdateNotificationMeasurementRuleRequest
import hs.flensburg.marlin.business.api.notificationMeasurementRule.entity.NotificationMeasurementRuleDTO
import hs.flensburg.marlin.business.api.users.boundary.UserService
import hs.flensburg.marlin.database.generated.tables.pojos.NotificationMeasurementRule
import java.time.LocalDateTime

object NotificationMeasurementRuleService {
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

    fun getNotificationMeasurementRule(id: Long): App<NotificationMeasurementRuleService.Error, NotificationMeasurementRuleDTO> = KIO.comprehension {
        NotificationMeasurementRuleRepo.fetchById(id).orDie().onNullFail { NotificationMeasurementRuleService.Error.NotFound }.map { NotificationMeasurementRuleDTO.from(it) }
    }

    fun getAllNotificationMeasurementRulesByLocationId(locationId: Long): App<NotificationMeasurementRuleService.Error, List<NotificationMeasurementRuleDTO>> = KIO.comprehension {
        NotificationMeasurementRuleRepo.fetchAllByLocationId(locationId).orDie().onNullFail { Error.NotFound } as KIO<JEnv, Error, List<NotificationMeasurementRuleDTO>>
    }

    fun getAllNotificationMeasurementRulesFromUser(userId: Long): App<NotificationMeasurementRuleService.Error, List<NotificationMeasurementRuleDTO>> = KIO.comprehension {
        NotificationMeasurementRuleRepo.fetchAllByUserId(userId).orDie().onNullFail { Error.NotFound } as KIO<JEnv, Error, List<NotificationMeasurementRuleDTO>>
    }

    fun getNotificationMeasurementRule(userId: Long, locationId: Long, measurementTypeId: Long) : App<NotificationMeasurementRuleService.Error, NotificationMeasurementRuleDTO> = KIO.comprehension {
        NotificationMeasurementRuleRepo.fetchByIds(userId, locationId, measurementTypeId).orDie().onNullFail { NotificationMeasurementRuleService.Error.NotFound }.map { NotificationMeasurementRuleDTO.from(it) }
    }

    fun createRule(
        rule: CreateOrUpdateNotificationMeasurementRuleRequest
    ): App<NotificationMeasurementRuleService.Error, NotificationMeasurementRuleDTO> = KIO.comprehension {
        val rule = !NotificationMeasurementRuleRepo.insert(
            NotificationMeasurementRule(
                userId = rule.userId,
                locationId = rule.locationId,
                measurementTypeId = rule.measurementTypeId,
                operator = rule.operator,
                measurementValue = rule.measurementValue,
                isActive = rule.isActive
            )
        ).orDie()
        NotificationMeasurementRuleRepo.fetchById(rule.id!!).orDie().onNullFail { NotificationMeasurementRuleService.Error.NotFound }.map { NotificationMeasurementRuleDTO.from(it) }
    }

    fun updateRule(
        id: Long,
        rule: CreateOrUpdateNotificationMeasurementRuleRequest
    ): App<NotificationMeasurementRuleService.Error, NotificationMeasurementRuleDTO> = KIO.comprehension {
        !NotificationMeasurementRuleRepo.update(
            id,
            NotificationMeasurementRule(
                userId = rule.userId,
                locationId = rule.locationId,
                measurementTypeId = rule.measurementTypeId,
                operator = rule.operator,
                measurementValue = rule.measurementValue,
                isActive = rule.isActive
            )
        ).orDie()
        NotificationMeasurementRuleRepo.fetchById(id).orDie().onNullFail { NotificationMeasurementRuleService.Error.NotFound }.map { NotificationMeasurementRuleDTO.from(it) }
    }

    fun deleteRule(ruleId: Long): App<UserService.Error, Unit> = KIO.comprehension {
        val rule = !NotificationMeasurementRuleRepo.fetchById(ruleId).orDie()
        NotificationMeasurementRuleRepo.deleteById(rule?.id!!).orDie()
    }
}