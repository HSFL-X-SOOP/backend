import de.lambda9.tailwind.core.KIO
import de.lambda9.tailwind.core.extensions.kio.onNullFail
import de.lambda9.tailwind.core.extensions.kio.orDie
import hs.flensburg.marlin.business.ApiError
import hs.flensburg.marlin.business.App
import hs.flensburg.marlin.business.JEnv
import hs.flensburg.marlin.business.ServiceLayerError
import hs.flensburg.marlin.business.api.notificationLocation.entity.CreateOrUpdateNotificationLocationRequest
import hs.flensburg.marlin.database.generated.tables.pojos.NotificationLocations

object NotificationLocationsService {
    sealed class Error(private val message: String) : ServiceLayerError {
        object NotFound : Error("Notification location not found")
        object BadRequest : Error("Bad request")

        override fun toApiError(): ApiError {
            return when (this) {
                is NotFound -> ApiError.NotFound(message)
                is BadRequest -> ApiError.BadRequest(message)
            }
        }
    }

    fun getNotificationLocation(id: Long): App<Error, NotificationLocationDTO> = KIO.comprehension {
        NotificationLocationsRepo.fetchById(id).orDie().onNullFail { Error.NotFound }.map { NotificationLocationDTO.from(it) }
    }

    fun getAllNotificationLocationsFromLocation(locationId: Long): App<Error, List<NotificationLocationDTO>> = KIO.comprehension {
        NotificationLocationsRepo.fetchAllByLocationId(locationId).orDie().onNullFail { Error.NotFound } as KIO<JEnv, Error, List<NotificationLocationDTO>>
    }

    fun create(
        notificationLocation: CreateOrUpdateNotificationLocationRequest
    ): App<Error, NotificationLocationDTO> = KIO.comprehension {
        val notificationLocation = !NotificationLocationsRepo.insert(
            NotificationLocations(
                locationId = notificationLocation.locationId,
                notificationTitle = notificationLocation.notificationTitle,
                notificationText = notificationLocation.notificationText,
                createdBy = notificationLocation.createdBy
            )
        ).orDie()
        NotificationLocationsRepo.fetchById(notificationLocation.id!!).orDie().onNullFail { Error.NotFound }.map { NotificationLocationDTO.from(it) }
    }

    fun delete(id: Long): App<NotificationLocationsService.Error, Unit> = KIO.comprehension {
        val notificationLocation = !NotificationLocationsRepo.fetchById(id).orDie()
        NotificationLocationsRepo.deleteById(notificationLocation?.id!!).orDie()
    }
}