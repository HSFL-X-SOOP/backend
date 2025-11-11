package hs.flensburg.marlin.business.api.location.boundary

import de.lambda9.tailwind.core.KIO
import de.lambda9.tailwind.core.extensions.kio.onNullFail
import de.lambda9.tailwind.core.extensions.kio.orDie
import de.lambda9.tailwind.jooq.transact
import hs.flensburg.marlin.business.ApiError
import hs.flensburg.marlin.business.App
import hs.flensburg.marlin.business.ServiceLayerError
import hs.flensburg.marlin.business.api.location.control.LocationRepo
import hs.flensburg.marlin.business.api.location.entity.DetailedLocationDTO
import hs.flensburg.marlin.business.api.location.entity.UpdateLocationRequest
import hs.flensburg.marlin.business.api.users.control.UserRepo
import hs.flensburg.marlin.database.generated.enums.UserAuthorityRole
import java.io.File

object LocationService {
    sealed class Error(private val message: String) : ServiceLayerError {
        object NotFound : Error("Location not found")
        object ImageNotFound : Error("Location image not found")
        object BadRequest : Error("Bad request")
        object Unauthorized : Error("Authorization required")
        class ValidationError(message: String) : Error(message)

        override fun toApiError(): ApiError {
            return when (this) {
                is NotFound -> ApiError.NotFound(message)
                is ImageNotFound -> ApiError.NotFound(message)
                is BadRequest -> ApiError.BadRequest(message)
                is Unauthorized -> ApiError.Unauthorized(message)
                is ValidationError -> ApiError.BadRequest(message)
            }
        }
    }

    fun getLocationByID(locationId: Long): App<Error, DetailedLocationDTO> = KIO.comprehension {
        val location = !LocationRepo.fetchLocationByID(locationId).orDie().onNullFail { Error.NotFound }
        KIO.ok(DetailedLocationDTO.fromLocation(location))
    }

    fun getHarborMasterAssignedLocation(userId: Long): App<Error, DetailedLocationDTO> = KIO.comprehension {
        val user = !UserRepo.fetchById(userId).orDie().onNullFail { Error.NotFound }

        !KIO.failOn(user.role != UserAuthorityRole.HARBOR_MASTER) { Error.Unauthorized }
        val assignedLocation = !UserRepo.fetchUserAssignedLocationId(userId).orDie().onNullFail { Error.NotFound }
        val location = !LocationRepo.fetchLocationByID(assignedLocation).orDie().onNullFail { Error.NotFound }

        KIO.ok(DetailedLocationDTO.fromLocation(location))
    }


    fun updateLocationByID(userId: Long, id: Long, request: UpdateLocationRequest): App<Error, DetailedLocationDTO> =
        KIO.comprehension {
            !checkLocationAccess(userId, id)

            val location = !LocationRepo.updateLocation(
                id = id,
                name = request.name.takeIf { it.isNotBlank() },
                description = request.description,
                address = request.address.takeIf { it.isNotBlank() },
                openingHours = request.openingHours,
                phone = request.contact?.phone,
                email = request.contact?.email,
                website = request.contact?.website
            ).orDie().onNullFail { Error.NotFound }

            if (!request.image?.base64.isNullOrBlank() && !request.image.contentType.isNullOrBlank()) {
                !KIO.failOn(imageTypeToExtension(request.image.contentType) == null) { Error.BadRequest }

                val imageBytes = request.image.base64.let {
                    java.util.Base64.getDecoder().decode(it)
                }

                val existingImage = !LocationRepo.fetchLocationImage(id = id).orDie()
                if (existingImage != null) {
                    !updateLocationImage(id, imageBytes, request.image.contentType)
                } else {
                    !createLocationImage(id, imageBytes, request.image.contentType)
                }
            }

            KIO.ok(DetailedLocationDTO.fromLocation(location))
        }.transact()

    fun getLocationImage(id: Long): App<Error, File> = KIO.comprehension {
        val locationImage = !LocationRepo.fetchLocationImage(id = id).orDie().onNullFail { Error.ImageNotFound }

        !KIO.failOn(locationImage.data == null || locationImage.contentType == null) { Error.ImageNotFound }

        val extension = imageTypeToExtension(locationImage.contentType!!)

        val tempFile = kotlin.io.path.createTempFile(suffix = extension).toFile()
        tempFile.writeBytes(locationImage.data!!)
        tempFile.deleteOnExit()

        KIO.ok(tempFile)
    }

    private fun updateLocationImage(id: Long, imageBytes: ByteArray, contentType: String): App<Error, Unit> =
        KIO.comprehension {
            LocationRepo.updateLocationImage(
                id = id,
                imageBytes = imageBytes,
                contentType = contentType
            ).orDie().onNullFail { Error.ImageNotFound }
        }

    private fun createLocationImage(id: Long, imageBytes: ByteArray, contentType: String): App<Error, Unit> =
        KIO.comprehension {
            LocationRepo.insertLocationImage(
                id = id,
                imageBytes = imageBytes,
                contentType = contentType
            ).orDie().onNullFail { Error.ImageNotFound }
        }

    fun deleteLocationImage(id: Long): App<Error, Unit> = KIO.comprehension {
        LocationRepo.deleteLocationImage(
            id = id
        ).orDie().onNullFail { Error.ImageNotFound }
    }

    private fun checkLocationAccess(userId: Long, locationId: Long): App<Error, Unit> = KIO.comprehension {
        val user = !UserRepo.fetchById(userId).orDie().onNullFail { Error.Unauthorized }
        // Admin can access all locations
        if (user.role == UserAuthorityRole.ADMIN) {
            return@comprehension KIO.unit
        }
        // Harbor master can only access their assigned location
        if (user.role == UserAuthorityRole.HARBOR_MASTER) {
            val assignedLocation =
                !UserRepo.fetchUserAssignedLocationId(userId).orDie().onNullFail { Error.Unauthorized }
            !KIO.failOn(locationId != assignedLocation) { Error.Unauthorized }
        } else {
            !KIO.fail(Error.Unauthorized)
        }
        KIO.unit
    }

    private fun imageTypeToExtension(contentType: String): String? {
        return when (contentType) {
            "image/apng" -> ".apng"
            "image/webp" -> ".webp"
            "image/avif" -> ".avif"
            "image/bmp" -> ".bmp"
            "image/vnd.microsoft.icon" -> ".ico"
            "image/tiff" -> ".tiff"
            "image/png" -> ".png"
            "image/jpeg" -> ".jpg"
            "image/gif" -> ".gif"
            "image/svg+xml" -> ".svg"
            else -> null
        }
    }
}