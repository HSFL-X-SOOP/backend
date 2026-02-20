package hs.flensburg.marlin.business.schedulerJobs.sensorData.boundary

import de.lambda9.tailwind.core.KIO
import de.lambda9.tailwind.core.extensions.kio.attempt
import de.lambda9.tailwind.jooq.transact
import hs.flensburg.marlin.business.App
import hs.flensburg.marlin.business.JEnv
import hs.flensburg.marlin.business.httpclient
import hs.flensburg.marlin.business.schedulerJobs.potentialSensors.boundary.PotentialSensorService
import hs.flensburg.marlin.business.schedulerJobs.sensorData.boundary.PreProcessingService.preProcessData
import hs.flensburg.marlin.business.schedulerJobs.sensorData.control.SensorDataRepo
import hs.flensburg.marlin.business.schedulerJobs.sensorData.entity.ThingClean
import hs.flensburg.marlin.business.schedulerJobs.sensorData.entity.ThingRaw
import hs.flensburg.marlin.business.schedulerJobs.sensorData.entity.toClean
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.appendPathSegments
import io.ktor.http.takeFrom
import kotlinx.coroutines.runBlocking
import java.time.OffsetDateTime
import java.time.ZoneOffset

private val logger = KotlinLogging.logger { }

object SensorDataService {

    fun getSensorDataFromActiveSensors(): App<PotentialSensorService.Error, Unit> = KIO.comprehension {
        val activeSensorIds = !PotentialSensorService.getActivePotentialSensorIds()

        getAndSaveAllSensorsData(activeSensorIds) { locationId ->
            // This block triggers if a new measurement is available for a location within the last hour
            KIO.comprehension {
                logger.debug { "New measurements for Location $locationId -> Trigger" }

                // TODO: Call other services here, like anomaly detection and notification

                KIO.unit
            }
        }
    }

    fun fetchSensorDataFrostServer(baseUrl: String, id: Long): ThingRaw = runBlocking {
        val expandValue = "Locations(\$select=location),Datastreams(" +
                "\$select=name,description,unitOfMeasurement,phenomenonTime,resultTime;" +
                "\$expand=Sensor(\$select=name,description,metadata)," +
                "ObservedProperty(\$select=name,description)," +
                "Observations(\$orderby=phenomenonTime+desc;\$top=1;\$select=phenomenonTime,result))"

        httpclient.get {
            url {
                takeFrom(baseUrl)
                appendPathSegments("Things($id)")
                encodedParameters.append("\$expand", expandValue)
            }
        }.body<ThingRaw>()
    }

    fun getAndSaveAllSensorsData(
        ids: List<Long>,
        onNewData: (Long) -> App<PotentialSensorService.Error, Unit> = { KIO.unit }
    ): App<PotentialSensorService.Error, Unit> = KIO.comprehension {
        val frostServerBaseUrl = (!KIO.access<JEnv>()).env.config.dataSources.FrostServerPath
        ids.forEach { id ->
            // Fetch Frost Server
            // Response to clean
            val thingClean = fetchSensorDataFrostServer(frostServerBaseUrl, id).toClean()

            // Preprocess the data
            val thingProcessed = preProcessData(thingClean)

            // Save the sensor data to the database
            val result = (!SensorDataRepo.saveSensorData(thingProcessed).transact().attempt()).fold(
                onSuccess = { it },
                onError = {
                    logger.error { it.toString() }
                    null
                })


            // Trigger if new measurement within 1 hour
            // Use for notification or anomaly detection
            if (result != null && result.newMeasurementsSaved && result.timestamp != null) {
                // Sensor data and db use UTC
                val oneHourAgo = OffsetDateTime.now(ZoneOffset.UTC).minusHours(1)

                if (result.timestamp.isAfter(oneHourAgo)) {
                    printStationInfo(id, result.locationId, thingClean)

                    (!onNewData(result.locationId).attempt()).fold(
                        onSuccess = { },
                        onError = { logger.error { it.toString() } })

                } else {
                    logger.debug { "Saved historical/delayed data for $id, skipping notification." }
                }
            }
        }
        KIO.unit
    }


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