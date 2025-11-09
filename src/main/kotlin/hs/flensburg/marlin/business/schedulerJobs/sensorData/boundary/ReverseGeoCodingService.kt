package hs.flensburg.marlin.business.schedulerJobs.sensorData.boundary

import de.lambda9.tailwind.core.KIO
import de.lambda9.tailwind.core.extensions.kio.fold
import de.lambda9.tailwind.core.extensions.kio.orDie
import de.lambda9.tailwind.core.extensions.kio.run
import hs.flensburg.marlin.business.ApiError
import hs.flensburg.marlin.business.App
import hs.flensburg.marlin.business.ServiceLayerError
import hs.flensburg.marlin.business.api.location.control.LocationRepo
import hs.flensburg.marlin.business.httpclient
import hs.flensburg.marlin.business.schedulerJobs.sensorData.entity.NominatimResponse
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration.Companion.seconds

object ReverseGeoCodingService {
    sealed class Error(private val message: String) : ServiceLayerError {
        object BadRequest : Error("Bad request")

        override fun toApiError(): ApiError {
            return when (this) {
                is BadRequest -> ApiError.BadRequest(message)
            }
        }
    }

    fun updateLocationNames(): App<Error, Unit> = KIO.comprehension {
        val locationWithoutNamesOrAddress = !LocationRepo.fetchLocationsWithoutNameOrAdressButCoordinates().orDie()
        locationWithoutNamesOrAddress.forEach { location ->
            // use delay (Nominatim Usage Policy)
            Thread.sleep(1.seconds.inWholeMilliseconds)

            val response = try {
                fetchLocationInfoFromNominatim(location.coordinates!!.lat, location.coordinates!!.lon)
            } catch (e: Exception) {
                println("Failed to fetch Location Name, Address for ${location.id}: ${e.message}")
                null
            }
            if (response == null) return@forEach

            var name = location.name ?: "Unknown Location"
            var address = location.address ?: "Unknown Address"


            if (response.display_name != null) {
                // Extract the name and address
                val parts = response.display_name.split(",").map { it.trim() }

                if (location.name.isNullOrBlank()) {
                    name = parts.firstOrNull().orEmpty()

                    val cityOrTown = response.address?.city
                        ?: response.address?.town
                        ?: response.address?.village
                        ?: response.address?.suburb
                        ?: ""

                    // Append city name if not already part of the name
                    if (!cityOrTown.isBlank()) {
                        val normalizedName = name.lowercase()
                        val normalizedCity = cityOrTown.lowercase()

                        if (!normalizedName.contains(normalizedCity)) {
                            name = "$name, $cityOrTown"
                        }
                    }
                }

                if (location.address.isNullOrBlank()) {
                    address = parts.drop(1).joinToString(", ").ifBlank { address }
                }
            }

            !LocationRepo.updateLocation(
                id = location.id!!,
                name = name,
                address = address,
                description = location.description,
                openingHours = location.openingHours,
                phone = location.contactPhone,
                email = location.contactEmail,
                website = location.contactWebsite
            ).run().fold(
                onFailure = { println("Failed to update Location Name, Address for ${location.id}: $it")},
                onSuccess = { }
            )
        }
        KIO.unit
    }

    fun fetchLocationInfoFromNominatim(lat: Double, lon: Double): NominatimResponse = runBlocking {
        httpclient.get("https://nominatim.openstreetmap.org/reverse") {
            parameter("lat", lat)
            parameter("lon", lon)
            parameter("format", "json")
        }.body<NominatimResponse>()
    }
}
