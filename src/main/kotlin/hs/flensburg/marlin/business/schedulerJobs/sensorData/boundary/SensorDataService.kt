package hs.flensburg.marlin.business.schedulerJobs.sensorData.boundary

import de.lambda9.tailwind.core.KIO
import de.lambda9.tailwind.core.extensions.kio.orDie
import de.lambda9.tailwind.jooq.transact
import hs.flensburg.marlin.business.App
import hs.flensburg.marlin.business.httpclient
import hs.flensburg.marlin.business.schedulerJobs.potentialSensors.boundary.PotentialSensorService
import hs.flensburg.marlin.business.schedulerJobs.sensorData.boundary.PreProcessingService.preProcessData
import hs.flensburg.marlin.business.schedulerJobs.sensorData.control.SensorDataRepo
import hs.flensburg.marlin.business.schedulerJobs.sensorData.entity.ThingClean
import hs.flensburg.marlin.business.schedulerJobs.sensorData.entity.ThingRaw
import hs.flensburg.marlin.business.schedulerJobs.sensorData.entity.toClean
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.runBlocking
import java.time.OffsetDateTime
import java.time.ZoneOffset

object SensorDataService {
    suspend fun getSensorData(id: Int = 10) {
        try {
            //val url = "https://timeseries.geomar.de/soop/FROST-Server/v1.1/Things(3)?\$expand=Locations(\$select=location),Datastreams(\$expand=Sensor,ObservedProperty,Observations(\$top=5))"
            val url =
                "https://timeseries.geomar.de/soop/FROST-Server/v1.1/Things(${id})?\$expand=Locations(\$select=location),Datastreams(\$select=name,description,unitOfMeasurement,phenomenonTime,resultTime;\$expand=Sensor(\$select=name,description,metadata),ObservedProperty(\$select=name,description),Observations(\$orderby=phenomenonTime+desc;\$top=1;\$select=phenomenonTime,result))"
            val thingRaw: ThingRaw = httpclient.get(url).body()
            val thingClean = thingRaw.toClean()
            println("Name: ${thingClean.name}, Location: ${thingClean.location}")

            val tideStream = thingClean.datastreams
                .find { it.observedProperty.name == "Tide" }

            val tideMeasurement = tideStream?.let {
                val measurement = it.measurements.firstOrNull()
                if (measurement != null) {
                    "Die aktuelle Tide beträgt ${measurement.result} ${it.unitOfMeasurement.symbol} " +
                            "gemessen am ${measurement.timestamp}"
                } else {
                    "Keine Tide-Messung verfügbar"
                }
            } ?: "Kein Tide-Datenstrom gefunden"

            println(tideMeasurement)

        } catch (e: Exception) {
            println("Fehler bei der Abfrage: ${e.message}")
        }
    }

    fun getSensorDataFromActiveSensors(
        onNewData: (Long) -> App<PotentialSensorService.Error, Unit> = { KIO.unit }
    ): App<PotentialSensorService.Error, Unit> = KIO.comprehension {
        val activeSensorIds = !PotentialSensorService.getActivePotentialSensorIds()
        getAndSaveAllSensorsData(activeSensorIds, onNewData)
    }

    fun fetchSensorDataFrostServer(id: Long): ThingRaw = runBlocking {
        val url =
            "https://timeseries.geomar.de/soop/FROST-Server/v1.1/Things($id)?\$expand=Locations(\$select=location),Datastreams(\$select=name,description,unitOfMeasurement,phenomenonTime,resultTime;\$expand=Sensor(\$select=name,description,metadata),ObservedProperty(\$select=name,description),Observations(\$orderby=phenomenonTime+desc;\$top=1;\$select=phenomenonTime,result))"
        httpclient.get(url).body<ThingRaw>()
    }

    fun getAndSaveAllSensorsData(
        ids: List<Long>,
        onNewData: (Long) -> App<PotentialSensorService.Error, Unit> = { KIO.unit }
    ): App<PotentialSensorService.Error, Unit> = KIO.comprehension {
        ids.forEach { id ->
            // Fetch Frost Server
            // Response to clean
            val thingClean = fetchSensorDataFrostServer(id).toClean()

            // Preprocess the data
            val thingProcessed = preProcessData(thingClean)

            // Save the sensor data to the database
            val result = !SensorDataRepo.saveSensorData(thingProcessed).orDie()

            // Trigger if new measurement within 1 hour
            // Use for notification or anomaly detection
            if (result.newMeasurementsSaved && result.timestamp != null) {
                // Sensor data and db use UTC
                val oneHourAgo = OffsetDateTime.now(ZoneOffset.UTC).minusHours(1)

                if (result.timestamp.isAfter(oneHourAgo)) {
                    printStationInfo(id, result.locationId, thingClean)
                    !onNewData(result.locationId)
                } else {
                    println("Saved historical/delayed data for $id, skipping notification.")
                }
            }
        }
        KIO.unit
    }.transact()


    private fun printStationInfo(id: Long, locationId: Long, thingClean: ThingClean) {
        println("\n=== Station ${thingClean.name} (ID: $id) ===")

        val tideMeasurement = formatTideMeasurement(thingClean)
        println(tideMeasurement)
        println("Position: ${thingClean.location}, LocationID: ${locationId}\n")
    }

    private fun formatTideMeasurement(thingClean: ThingClean): String {
        val tideStream = thingClean.datastreams
            .find { it.observedProperty.name == "Tide" }

        return tideStream?.let {
            val measurement = it.measurements.firstOrNull()
            if (measurement != null) {
                "Die aktuelle Tide beträgt ${measurement.result} ${it.unitOfMeasurement.symbol} " +
                        "(${it.unitOfMeasurement.name}), " +
                        "gemessen am ${measurement.timestamp}"
            } else {
                "Keine Tide-Messung verfügbar"
            }
        } ?: "Kein Tide-Datenstrom gefunden"
    }
}