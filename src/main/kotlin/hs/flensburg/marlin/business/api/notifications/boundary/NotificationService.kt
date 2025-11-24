package hs.flensburg.marlin.business.api.notifications.boundary

import de.lambda9.tailwind.core.KIO
import de.lambda9.tailwind.core.extensions.kio.orDie
import hs.flensburg.marlin.business.ApiError
import hs.flensburg.marlin.business.App
import hs.flensburg.marlin.business.ServiceLayerError
import hs.flensburg.marlin.business.api.location.control.LocationRepo
import hs.flensburg.marlin.business.api.notificationMeasurementRule.control.NotificationMeasurementRuleRepo
import hs.flensburg.marlin.business.api.notificationMeasurementRule.entity.NotificationMeasurementRuleDTO
import hs.flensburg.marlin.business.api.sensors.control.SensorRepo
import hs.flensburg.marlin.business.api.sensors.entity.EnrichedMeasurementDTO
import hs.flensburg.marlin.business.api.sensors.entity.LocationWithLatestMeasurementsDTO
import hs.flensburg.marlin.business.schedulerJobs.sensorData.boundary.ReverseGeoCodingService
import hs.flensburg.marlin.database.generated.tables.pojos.Location


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
        val locationWithLatestMeasurements: List<LocationWithLatestMeasurementsDTO> = !SensorRepo.fetchLocationsWithLatestMeasurements("").orDie()
        locations.forEach { location ->
            val currentLocationWithMeasurements: List<LocationWithLatestMeasurementsDTO> = locationWithLatestMeasurements.filter { it.location.id == location!!.id }
            if (currentLocationWithMeasurements.isEmpty()) { return@forEach }
            val currentLocationWithMeasurement: LocationWithLatestMeasurementsDTO = currentLocationWithMeasurements[0]


            val notificationMeasurementRules: List<NotificationMeasurementRuleDTO?> = !NotificationMeasurementRuleRepo.fetchAllByLocationId(location!!.id!!).orDie()
            notificationMeasurementRules.forEach{notificationMeasurementRule ->
                val enrichedMeasurementDTOs: List<EnrichedMeasurementDTO> = currentLocationWithMeasurement.latestMeasurements.filter { it.measurementType.id == notificationMeasurementRule!!.measurementTypeId }
                if (enrichedMeasurementDTOs.isEmpty()) { return@forEach }
                val enrichedMeasurementDTO: EnrichedMeasurementDTO = enrichedMeasurementDTOs[0]

                val measurementValue = enrichedMeasurementDTO.value
                val operator = notificationMeasurementRule!!.operator
                val notificationValue = notificationMeasurementRule.measurementValue

                val sentNotification = when (operator) {
                    "<" -> measurementValue < notificationValue
                    ">" -> measurementValue > notificationValue
                    "<=" -> measurementValue <= notificationValue
                    ">=" -> measurementValue >= notificationValue
                    else -> false
                }

                if (sentNotification) {
//                    val userDevices: List<UserDevice?> = !UserDeviceRepo.fetchAllByUserId(notificationMeasurementRule.userId).orDie()
//                    userDevices.forEach { userDevice ->
//                        FirebaseNotificationSender.sendNotification(
//                            token = userDevice!!.fcmToken,
//                            title = "${currentLocationWithMeasurements.location.name}",
//                            message = "${va.measurementType.name} $operator $notificationValue"
//                        )
//                    }
                    println("${currentLocationWithMeasurement.location.name} ${enrichedMeasurementDTO.measurementType.name} Sent notification: MV $measurementValue $operator NV $notificationValue ")
                }
            }

        }
        KIO.unit
    }



}