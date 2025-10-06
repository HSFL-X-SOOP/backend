package hs.flensburg.marlin.business.schedulerJobs.potentialSensors.boundary

import de.lambda9.tailwind.core.Exit.Companion.isSuccess
import de.lambda9.tailwind.core.KIO
import de.lambda9.tailwind.core.KIO.Companion.unsafeRunSync
import de.lambda9.tailwind.core.extensions.kio.onNullFail
import de.lambda9.tailwind.core.extensions.kio.orDie
import hs.flensburg.marlin.business.ApiError
import hs.flensburg.marlin.business.App
import hs.flensburg.marlin.business.JEnv
import hs.flensburg.marlin.business.ServiceLayerError
import hs.flensburg.marlin.business.httpclient
import hs.flensburg.marlin.business.schedulerJobs.potentialSensors.control.PotentialSensorRepo
import hs.flensburg.marlin.business.schedulerJobs.potentialSensors.entity.FrostResponse
import hs.flensburg.marlin.business.schedulerJobs.potentialSensors.entity.PotentialSensorResponse
import hs.flensburg.marlin.database.generated.tables.pojos.PotentialSensor
import io.ktor.client.call.body
import io.ktor.client.request.get

object PotentialSensorService {
    sealed class Error(private val message: String) : ServiceLayerError {
        object NotFound : Error("User profile not found")
        object BadRequest : Error("Bad request")

        override fun toApiError(): ApiError {
            return when (this) {
                is NotFound -> ApiError.NotFound(message)
                is BadRequest -> ApiError.BadRequest(message)
            }
        }
    }
    suspend fun getAllPotentialSensors(env: JEnv){
        try {
            // all things with id, name, description, without links
            val url = "https://timeseries.geomar.de/soop/FROST-Server/v1.1/Things?\$select=@iot.id,name,description"
            // Response is wrapped in "value"
            val frostResponse: FrostResponse<PotentialSensorResponse> = httpclient.get(url).body()
            val sensors: List<PotentialSensorResponse> = frostResponse.value
            // Set 'isSensor' flag
            val markedSensors = markSensors(sensors)
            markedSensors.forEach {
                println("ID: ${it.id}, Name: ${it.name}, isSensor: ${it.isSensor}")
            }
            //save sensors
            val result = savePotentialSensors(markedSensors).unsafeRunSync(env)
            if (result.isSuccess()){
                println("Gespeichert: $result")
            }
        } catch (e: Exception) {
            println("Fehler bei der Abfrage: ${e.message}")
        }
    }

    fun savePotentialSensors(
        sensors: List<PotentialSensorResponse>
    ): App<Error, List<PotentialSensor>> = KIO.comprehension {

        val maxId = !PotentialSensorRepo.fetchMaxPotentialSensorId().orDie()

        val newSensors = if (maxId != null) {
            println("Max ID in DB: $maxId")
            sensors.filter { it.id > maxId }
        } else {
            println("No sensors in DB yet.")
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

    fun getActivePotentialSensorIds(): App<Error, List<Long>> = KIO.comprehension {
        val ids = !PotentialSensorRepo.fetchActivePotentialSensorIds().orDie().onNullFail { Error.NotFound }
        KIO.ok(ids)
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