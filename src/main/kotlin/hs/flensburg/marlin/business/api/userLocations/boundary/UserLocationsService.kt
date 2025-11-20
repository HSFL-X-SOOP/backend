import de.lambda9.tailwind.core.KIO
import de.lambda9.tailwind.core.extensions.kio.onNullFail
import de.lambda9.tailwind.core.extensions.kio.orDie
import hs.flensburg.marlin.business.ApiError
import hs.flensburg.marlin.business.App
import hs.flensburg.marlin.business.JEnv
import hs.flensburg.marlin.business.ServiceLayerError
import hs.flensburg.marlin.business.api.userLocations.entity.CreateOrUpdateUserLocationRequest
import hs.flensburg.marlin.business.api.users.boundary.UserService
import hs.flensburg.marlin.database.generated.tables.pojos.UserLocations

object UserLocationsService {
    sealed class Error(private val message: String) : ServiceLayerError {
        object NotFound : Error("User location not found")
        object BadRequest : Error("Bad request")

        override fun toApiError(): ApiError {
            return when (this) {
                is NotFound -> ApiError.NotFound(message)
                is BadRequest -> ApiError.BadRequest(message)
            }
        }
    }

    fun getUserLocation(id: Long): App<UserLocationsService.Error, UserLocationDTO> = KIO.comprehension {
        UserLocationsRepo.fetchById(id).orDie().onNullFail { UserLocationsService.Error.NotFound }.map { UserLocationDTO.from(it) }
    }

    fun getAllUserLocationsFromUser(userId: Long): App<UserLocationsService.Error, List<UserLocationDTO>> = KIO.comprehension {
        UserLocationsRepo.fetchAllByUserId(userId).orDie().onNullFail { UserLocationsService.Error.NotFound } as KIO<JEnv, UserLocationsService.Error, List<UserLocationDTO>>
    }

    fun create(
        userLocation: CreateOrUpdateUserLocationRequest
    ): App<UserLocationsService.Error, UserLocationDTO> = KIO.comprehension {
        val userLocation = !UserLocationsRepo.insert(
            UserLocations(
                userId = userLocation.userId,
                locationId = userLocation.locationId,
                sentHarborNotifications = userLocation.sentHarborNotifications,
            )
        ).orDie()
        UserLocationsRepo.fetchById(userLocation.id!!).orDie().onNullFail { UserLocationsService.Error.NotFound }.map { UserLocationDTO.from(it) }
    }

    fun update(
        id: Long,
        userLocation: CreateOrUpdateUserLocationRequest
    ): App<UserLocationsService.Error, UserLocationDTO> = KIO.comprehension {
        !UserLocationsRepo.update(
            id,
            UserLocations(
                userId = userLocation.userId,
                locationId = userLocation.locationId,
                sentHarborNotifications = userLocation.sentHarborNotifications,
            )
        ).orDie()
        UserLocationsRepo.fetchById(id).orDie().onNullFail { UserLocationsService.Error.NotFound }.map { UserLocationDTO.from(it) }
    }

    fun delete(id: Long): App<UserService.Error, Unit> = KIO.comprehension {
        val userLocation = !UserLocationsRepo.fetchById(id).orDie()
        UserLocationsRepo.deleteById(userLocation?.id!!).orDie()
    }
}