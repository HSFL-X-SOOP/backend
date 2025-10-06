package hs.flensburg.marlin.business.schedulerJobs.potentialSensors.boundary

import de.lambda9.tailwind.core.KIO
import de.lambda9.tailwind.core.extensions.kio.onNullFail
import de.lambda9.tailwind.core.extensions.kio.orDie
import hs.flensburg.marlin.business.ApiError
import hs.flensburg.marlin.business.App
import hs.flensburg.marlin.business.ServiceLayerError
import hs.flensburg.marlin.business.api.potentialSensors.entity.PotentialSensorDTO
import hs.flensburg.marlin.business.api.potentialSensors.entity.toDTO
import hs.flensburg.marlin.business.httpclient
import hs.flensburg.marlin.business.schedulerJobs.potentialSensors.control.PotentialSensorRepo
import hs.flensburg.marlin.business.schedulerJobs.potentialSensors.entity.FrostResponse
import hs.flensburg.marlin.business.schedulerJobs.potentialSensors.entity.PotentialSensorResponse
import hs.flensburg.marlin.database.generated.tables.pojos.PotentialSensor
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.runBlocking

object PotentialSensorService {
    sealed class Error(private val message: String) : ServiceLayerError {
        object NotFound : Error("Sensor not found")
        object BadRequest : Error("Bad request")

        override fun toApiError(): ApiError {
            return when (this) {
                is NotFound -> ApiError.NotFound(message)
                is BadRequest -> ApiError.BadRequest(message)
            }
        }
    }

    fun fetchPotentialSensorsFrostServer(): FrostResponse<PotentialSensorResponse> = runBlocking {
        // all things with id, name, description, without links
        val url = "https://timeseries.geomar.de/soop/FROST-Server/v1.1/Things?\$select=@iot.id,name,description"
        httpclient.get(url).body<FrostResponse<PotentialSensorResponse>>()
    }

    fun getAndSaveAllPotentialSensors(): App<Error, Unit> = KIO.comprehension {
        // Fetch from FROST server
        // Response is wrapped in 'value'
        val sensors = fetchPotentialSensorsFrostServer().value

        // Set 'isSensor' flag
        val markedSensors = markSensors(sensors)

        // Save to DB
        val newSensors = !savePotentialSensors(markedSensors)

        // TODO: Notification if new sensors were added
        newSensors.forEach {
            println("\u001B[36mNew Potential Sensor - ID: ${it.id}, Name: ${it.name}, isActive: ${it.isActive}\u001B[0m")
        }

        KIO.unit
    }


    fun savePotentialSensors(
        sensors: List<PotentialSensorResponse>
    ): App<Error, List<PotentialSensor>> = KIO.comprehension {

        val maxId = !PotentialSensorRepo.fetchMaxPotentialSensorId().orDie()

        val newSensors =
            if (maxId != null) {
            sensors.filter { it.id > maxId }
        } else {
            sensors
        }

        val inserted = newSensors.map { sensor ->
            !PotentialSensorRepo.insertPotentialSensor(
                id = sensor.id.toLong(),
                name = sensor.name,
                description = sensor.description,
                isActive = sensor.isSensor
            ).orDie()
        }

        KIO.ok(inserted)
    }

    fun getAllPotentialSensors(): App<Error, List<PotentialSensorDTO>> = KIO.comprehension {
        val sensors = !PotentialSensorRepo.fetchAllPotentialSensors().orDie().onNullFail { Error.NotFound }
        KIO.ok(sensors.map { it.toDTO() })
    }

    fun getActivePotentialSensorIds(): App<Error, List<Long>> = KIO.comprehension {
        val ids = !PotentialSensorRepo.fetchActivePotentialSensorIds().orDie().onNullFail { Error.NotFound }
        KIO.ok(ids)
    }

    fun toggleIsActive(id: Long): App<Error, PotentialSensorDTO> = KIO.comprehension {
        val sensor = !PotentialSensorRepo.updateIsActive(id).orDie().onNullFail { Error.NotFound }
        KIO.ok(sensor.toDTO())
    }

    // Filters the list to only include items that look like sensors
    fun filterForSensors(sensors: List<PotentialSensorResponse>): List<PotentialSensorResponse> =
        sensors.filter { looksLikeSensor(it.name, it.description) }

    // Sets isSensor flag based on heuristics
    private fun markSensors(sensors: List<PotentialSensorResponse>): List<PotentialSensorResponse> =
        sensors.map { sensor ->
            sensor.copy(isSensor = looksLikeSensor(sensor.name, sensor.description))
        }

    // Heuristic to determine if a thing is likely a sensor
    private fun looksLikeSensor(name: String, description: String): Boolean {
        val desc = description.lowercase()
        val name = name.lowercase()

        val isSensorName = listOf("box_gmr_twl", "metbox").any { name.startsWith(it) }
        val isSensorDesc = listOf("temperature", "water level", "lorawan", "metbox", "measurement box").any { it in desc }
        val isNotVessel = !listOf("sailing vessel", "mmsi", "loa", "flag", "moving").any { it in desc }

        return (isSensorName || isSensorDesc) && isNotVessel
    }

}