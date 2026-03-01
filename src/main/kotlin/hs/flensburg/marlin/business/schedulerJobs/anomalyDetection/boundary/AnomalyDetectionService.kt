package hs.flensburg.marlin.business.schedulerJobs.anomalyDetection.boundary

import de.lambda9.tailwind.core.KIO
import de.lambda9.tailwind.core.extensions.kio.onNullFail
import de.lambda9.tailwind.core.extensions.kio.orDie
import hs.flensburg.marlin.business.ApiError
import hs.flensburg.marlin.business.App
import hs.flensburg.marlin.business.ServiceLayerError
import hs.flensburg.marlin.business.api.sensors.control.SensorRepo
import hs.flensburg.marlin.business.api.sensors.entity.EnrichedMeasurementDTO
import hs.flensburg.marlin.business.api.sensors.entity.LocationWithBoxesDTO
import hs.flensburg.marlin.business.api.sensors.entity.LocationWithLatestMeasurementsDTO
import hs.flensburg.marlin.business.api.sensors.entity.toLocationWithBoxesDTO
import java.security.MessageDigest

object AnomalyDetectionService {
    sealed class Error(private val message: String) : ServiceLayerError {
        object NotFound : Error("Location, Sensor or measurement not found")

        override fun toApiError(): ApiError {
            return when (this) {
                is NotFound -> ApiError.NotFound(message)
            }
        }
    }

    // Letzte 5 bekannte Hashes pro ID speichern
    val history = mutableListOf<String>()

    fun checkNewMeasurements(locationId: Long): App<ServiceLayerError, Unit> = KIO.comprehension {
        // Abfrage des aktuellsten Werts
        val latestMeasurements =
            !SensorRepo.fetchSingleLocationWithLatestMeasurements(locationId, "UTC", "metric").orDie()
                .onNullFail { Error.NotFound }
        val currentMeasurement = latestMeasurements.first()

        val hash = getHash(currentMeasurement)

        // Check, ob der aktuelle Wert bekannt ist
        if (hash !in history) {
            val pastMeasurements =
                !SensorRepo.fetchMeasurementsWithinCustomTimeRange(locationId, "3h", "UTC", "metric").orDie()
                    .onNullFail { Error.NotFound }

            // Altes Measurement
            detectAnomaly(
                currentMeasurement.latestMeasurements,
                pastMeasurements.latestMeasurements - currentMeasurement.latestMeasurements.toSet()
            )
            // Nein -> detectAnomaly() + Hash speichern
            history.addLast(hash)
        }
        if (history.size > 300) history.removeFirst()
        KIO.unit
    }

    private fun detectAnomaly(
        measurement: List<EnrichedMeasurementDTO>, pastMeasurements: List<EnrichedMeasurementDTO>
    ) {
        // Formatierung der Listen
        // Prüfung von physikalischen Limits & Änderungen
        // Prüfung von verwandten Messwerten
        // Prüfung von anderen Sensoren
    }

    private fun getHash(measurement: LocationWithLatestMeasurementsDTO): String {
        val bytes = measurement.toString().toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)

        return digest.joinToString("") { "%02x".format(it) }
    }
}