package hs.flensburg.marlin.business.api.location.boundary

import de.lambda9.tailwind.core.KIO
import de.lambda9.tailwind.core.extensions.kio.onNullFail
import de.lambda9.tailwind.core.extensions.kio.orDie
import hs.flensburg.marlin.business.ApiError
import hs.flensburg.marlin.business.App
import hs.flensburg.marlin.business.ServiceLayerError
import hs.flensburg.marlin.business.api.location.control.LocationRepo
import hs.flensburg.marlin.business.api.location.entity.DetailedLocationDTO
import hs.flensburg.marlin.business.api.location.entity.toDetailedLocationDTO

object LocationService {
    sealed class Error(private val message: String) : ServiceLayerError {
        object NotFound : Error("Location not found")
        object BadRequest : Error("Bad request")

        override fun toApiError(): ApiError {
            return when (this) {
                is NotFound -> ApiError.NotFound(message)
                is BadRequest -> ApiError.BadRequest(message)
            }
        }
    }

    fun getLocationByID(id: Long): App<Error, DetailedLocationDTO> = KIO.comprehension {
        val location = !LocationRepo.fetchLocationByID(id).orDie().onNullFail { Error.NotFound }
        KIO.ok(location.toDetailedLocationDTO())
    }
}