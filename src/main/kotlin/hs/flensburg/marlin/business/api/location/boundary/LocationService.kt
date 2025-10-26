package hs.flensburg.marlin.business.api.location.boundary

import de.lambda9.tailwind.core.KIO
import de.lambda9.tailwind.core.extensions.kio.onNullFail
import de.lambda9.tailwind.core.extensions.kio.orDie
import hs.flensburg.marlin.business.ApiError
import hs.flensburg.marlin.business.App
import hs.flensburg.marlin.business.ServiceLayerError
import hs.flensburg.marlin.business.api.location.control.LocationRepo
import hs.flensburg.marlin.business.api.location.entity.DetailedLocationDTO
import hs.flensburg.marlin.business.api.location.entity.UpdateLocationRequest
import hs.flensburg.marlin.business.api.location.entity.toDetailedLocationDTO
import hs.flensburg.marlin.business.api.timezones.boundary.toJavaLocalTime

object LocationService {
    sealed class Error(private val message: String) : ServiceLayerError {
        object NotFound : Error("Location not found")
        object ImageNotFound : Error("Location image not found")
        object BadRequest : Error("Bad request")

        override fun toApiError(): ApiError {
            return when (this) {
                is NotFound -> ApiError.NotFound(message)
                is ImageNotFound -> ApiError.NotFound(message)
                is BadRequest -> ApiError.BadRequest(message)
            }
        }
    }

    fun getLocationByID(id: Long): App<Error, DetailedLocationDTO> = KIO.comprehension {
        val location = !LocationRepo.fetchLocationByID(id).orDie().onNullFail { Error.NotFound }
        KIO.ok(location.toDetailedLocationDTO())
    }

    fun updateLocationByID(id: Long, request: UpdateLocationRequest): App<Error, DetailedLocationDTO> = KIO.comprehension {
        !KIO.failOn(request.name.isNullOrBlank()) { Error.BadRequest }
        val location = !LocationRepo.updateLocation(
            id = id,
            name = request.name?.takeIf { it.isNotBlank() },
            description = request.description,
            address = request.address,
            openingTime = request.openingTime?.toJavaLocalTime(),
            closingTime = request.closingTime?.toJavaLocalTime()
        ).orDie().onNullFail { Error.NotFound }
        KIO.ok(location.toDetailedLocationDTO())
    }

    fun getLocationImage(id: Long): App<Error, ByteArray?> = KIO.comprehension {
        val locationImage = !LocationRepo.fetchLocationImage(id = id).orDie().onNullFail { Error.ImageNotFound }
        KIO.ok(locationImage.image)
    }

    fun updateLocationImage(id: Long, imageBytes: ByteArray): App<Error, Unit> = KIO.comprehension {
        !LocationRepo.updateLocationImage(
            id = id,
            imageBytes = imageBytes
        ).orDie().onNullFail { Error.ImageNotFound }
        KIO.unit
    }

    fun createLocationImage(id: Long, imageBytes: ByteArray): App<Error, Unit> = KIO.comprehension {
        !LocationRepo.insertLocationImage(
            id = id,
            imageBytes = imageBytes
        ).orDie().onNullFail { Error.ImageNotFound }
        KIO.unit
    }
    fun deleteLocationImage(id: Long): App<Error, Unit> = KIO.comprehension {
        !LocationRepo.deleteLocationImage(
            id = id
        ).orDie().onNullFail { Error.ImageNotFound }
        KIO.unit
    }
}