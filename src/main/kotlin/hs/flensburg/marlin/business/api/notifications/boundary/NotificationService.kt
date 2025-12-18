package hs.flensburg.marlin.business.api.notifications.boundary

import de.lambda9.tailwind.core.KIO
import de.lambda9.tailwind.core.extensions.kio.orDie
import hs.flensburg.marlin.business.ApiError
import hs.flensburg.marlin.business.App
import hs.flensburg.marlin.business.ServiceLayerError
import hs.flensburg.marlin.business.api.location.control.LocationRepo
import hs.flensburg.marlin.business.api.notificationMeasurementRule.control.NotificationMeasurementRuleRepo
import hs.flensburg.marlin.business.api.notificationMeasurementRule.entity.NotificationMeasurementRuleDTO
import hs.flensburg.marlin.business.api.notificationMeasurementRule.entity.NotificationMeasurementRuleMessage
import hs.flensburg.marlin.business.api.notifications.NotificationSender
import hs.flensburg.marlin.business.api.sensors.control.SensorRepo
import hs.flensburg.marlin.business.api.sensors.entity.EnrichedMeasurementDTO
import hs.flensburg.marlin.business.api.sensors.entity.LocationWithLatestMeasurementsDTO
import hs.flensburg.marlin.business.api.userDevice.control.UserDeviceRepo
import hs.flensburg.marlin.business.api.userDevice.entity.UserDevice
import hs.flensburg.marlin.business.schedulerJobs.sensorData.boundary.ReverseGeoCodingService
import hs.flensburg.marlin.database.generated.tables.pojos.Location
import hs.flensburg.marlin.database.generated.tables.pojos.NotificationMeasurementRule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.datetime.toJavaLocalDateTime
import java.time.LocalDateTime


object NotificationService {
    sealed class Error(private val message: String) : ServiceLayerError {
        object NotFound : Error("Notification not found")
        object BadRequest : Error("Bad request")

        override fun toApiError(): ApiError {
            return when (this) {
                is NotFound -> ApiError.NotFound(message)
                is BadRequest -> ApiError.BadRequest(message)
            }
        }
    }

    fun sentNotificationMeasurementRules(): App<ReverseGeoCodingService.Error, Unit> = KIO.comprehension {
        val locations: List<Location?> = !LocationRepo.fetchAllLocations().orDie()
        val locationWithLatestMeasurements: List<LocationWithLatestMeasurementsDTO> = !SensorRepo.fetchLocationsWithLatestMeasurements("", units = "metric").orDie()
        //Go through all Locations
        locations.forEach { location ->
            //Get current location to access measurement values
            val currentLocationWithMeasurements: List<LocationWithLatestMeasurementsDTO> = locationWithLatestMeasurements.filter { it.location.id == location!!.id }
            if (currentLocationWithMeasurements.isEmpty()) { return@forEach }
            val currentLocationWithMeasurement: LocationWithLatestMeasurementsDTO = currentLocationWithMeasurements[0]

            //Get all NotificationMeasurementRules from Location and group them by UserId
            val notificationMeasurementRules: List<NotificationMeasurementRuleDTO?> = !NotificationMeasurementRuleRepo.fetchAllByLocationId(location!!.id!!).orDie()
            val groupedByUserIdNotificationMeasurementRules = notificationMeasurementRules.groupBy { it!!.userId }

            //Go through all NotificationMeasurementRules to check, if rules are true
            groupedByUserIdNotificationMeasurementRules.forEach{ (userId, userNotificationMeasurementRule) ->
                val notifications = mutableListOf<NotificationMeasurementRuleMessage>()
                userNotificationMeasurementRule.forEach { notificationMeasurementRule ->
                    if (!notificationMeasurementRule!!.isActive) {return@forEach}

                    //Get the needed measurement values for the current rule
                    val enrichedMeasurementDTOs: List<EnrichedMeasurementDTO> = currentLocationWithMeasurement.latestMeasurements.filter { it.measurementType.id == notificationMeasurementRule.measurementTypeId }
                    if (enrichedMeasurementDTOs.isEmpty()) { return@forEach }
                    val enrichedMeasurementDTO: EnrichedMeasurementDTO = enrichedMeasurementDTOs[0]

                    //Set measurement values of current location
                    val measurementValue = enrichedMeasurementDTO.value
                    val operator = notificationMeasurementRule.operator
                    val notificationValue = notificationMeasurementRule.measurementValue

                    //Set state values of notificationMeasurementRule
                    var lastState = notificationMeasurementRule.lastState
                    var lastNotifiedAt: LocalDateTime? = notificationMeasurementRule.lastNotifiedAt?.toJavaLocalDateTime()

                    //Check if the current NotificationMeasurementRule is valid
                    val sendNotification = validateNotificationMeasurementRule(operator, measurementValue, notificationValue)

                    if (!sendNotification) {
                        lastState = false
                    }

                    if (sendNotification && !lastState ) {
                        lastState = true
                        lastNotifiedAt = LocalDateTime.now()
                        notifications.add(NotificationMeasurementRuleMessage(
                            notificationMeasurementRule,
                            enrichedMeasurementDTO.measurementType.name,
                            enrichedMeasurementDTO.measurementType.unitSymbol,
                            enrichedMeasurementDTO.value,
                            enrichedMeasurementDTO.time
                            )
                        )
                    }

                    //NotificationMeasurementRules lastState und lastNotifiedAt update
                    val updatedNotificationMeasurementRule = NotificationMeasurementRule(
                        id = notificationMeasurementRule.id,
                        userId = notificationMeasurementRule.userId,
                        locationId = notificationMeasurementRule.locationId,
                        measurementTypeId = notificationMeasurementRule.measurementTypeId,
                        operator = notificationMeasurementRule.operator,
                        measurementValue = notificationMeasurementRule.measurementValue,
                        isActive = notificationMeasurementRule.isActive,
                        lastNotifiedAt = lastNotifiedAt,
                        lastState = lastState
                    )
                    !NotificationMeasurementRuleRepo.updateWhenNotify(notificationMeasurementRule.id, updatedNotificationMeasurementRule).orDie()
                }

                //Create a Notification Message from all valid measurementRules and send a Push-Notification to all registered Devices of a User
                val message = createMeasurementRuleNotificationMessage(notifications)
                val userDevices: List<UserDevice?> = !UserDeviceRepo.fetchAllByUserId(userId).orDie()
                CoroutineScope(Dispatchers.IO).launch {
                    sendNotificationToAllUserDevices(userDevices, message, currentLocationWithMeasurement.location.name)
                }
            }
        }
        KIO.unit
    }

    fun createMeasurementRuleNotificationMessage(notifications: List<NotificationMeasurementRuleMessage>): String {
        var message = ""
        notifications.map { notification ->
            message += "${notification.measurementType}: ${notification.measurementValue} ${notification.measurementUnitSymbol} \n"
        }
        return message
    }

    suspend fun sendNotificationToAllUserDevices(userDevices: List<UserDevice?>, message: String, currentLocation: String?) {
        if (message != "") {
            userDevices.forEach { userDevice ->
                NotificationSender.sendNotification(
                    expoToken = userDevice!!.fcmToken,
                    title = "$currentLocation",
                    body = message
                )
            }
        }
    }

    fun validateNotificationMeasurementRule(operator: String, measurementValue: Double, notificationValue: Double): Boolean {
        return when (operator) {
            "<" -> measurementValue < notificationValue
            ">" -> measurementValue > notificationValue
            "<=" -> measurementValue <= notificationValue
            ">=" -> measurementValue >= notificationValue
            else -> false
        }
    }
}