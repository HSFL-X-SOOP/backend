package hs.flensburg.marlin.business.schedulerJobs.httpTestJob.boundary

import de.lambda9.tailwind.core.KIO.Companion.unsafeRunSync
import hs.flensburg.marlin.business.JEnv
import hs.flensburg.marlin.business.httpclient
import hs.flensburg.marlin.business.schedulerJobs.httpTestJob.boundary.PreProcessingService.preProcessData
import hs.flensburg.marlin.business.schedulerJobs.httpTestJob.control.SensorDataRepo
import hs.flensburg.marlin.business.schedulerJobs.httpTestJob.entity.ThingClean
import hs.flensburg.marlin.business.schedulerJobs.httpTestJob.entity.ThingRaw
import hs.flensburg.marlin.business.schedulerJobs.httpTestJob.entity.toClean
import io.ktor.client.call.*
import io.ktor.client.request.*

object SensorDataService {
    suspend fun getSensorData(id : Int=10){
        try {
            //val url = "https://timeseries.geomar.de/soop/FROST-Server/v1.1/Things(3)?\$expand=Locations(\$select=location),Datastreams(\$expand=Sensor,ObservedProperty,Observations(\$top=5))"
            val url = "https://timeseries.geomar.de/soop/FROST-Server/v1.1/Things(${id})?\$expand=Locations(\$select=location),Datastreams(\$select=name,description,unitOfMeasurement,phenomenonTime,resultTime;\$expand=Sensor(\$select=name,description,metadata),ObservedProperty(\$select=name,description),Observations(\$orderby=phenomenonTime+desc;\$top=1;\$select=phenomenonTime,result))"
            val thingRaw: ThingRaw = httpclient.get(url).body()
            val thingClean = thingRaw.toClean()
            println("Name: ${thingClean.name}, Location: ${thingClean.location}")

            val tideStream = thingClean.datastreams
                .find { it.observedProperty.name == "Tide" }
            
            val tideMeasurement = tideStream?.let {
                val measurement = it.measurements.firstOrNull()
                if (measurement != null) {
                    "Die aktuelle Tide beträgt ${measurement.result} ${it.unitOfMeasurement.symbol} "+
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

    suspend fun getMultipleSensorData(idRange : IntRange = 3..12, env: JEnv) {
        for (id in idRange) {
            //  fetch sensor data for each id in the range
            try {
                val url = "https://timeseries.geomar.de/soop/FROST-Server/v1.1/Things($id)?\$expand=Locations(\$select=location),Datastreams(\$select=name,description,unitOfMeasurement,phenomenonTime,resultTime;\$expand=Sensor(\$select=name,description,metadata),ObservedProperty(\$select=name,description),Observations(\$orderby=phenomenonTime+desc;\$top=1;\$select=phenomenonTime,result))"
                val thingRaw: ThingRaw = httpclient.get(url).body()
                val thingClean: ThingClean = thingRaw.toClean()

                // Preprocess the data
                val thingProcessed = preProcessData(thingClean)
                println("PreProcessed: $thingProcessed")

                // Save the sensor data to the database
                val test = SensorDataRepo.saveSensorData(thingProcessed).unsafeRunSync(env)

                // Print the station information
                printStationInfo(id, thingProcessed, test)

            } catch (e: Exception) {
                println("\n=== ID $id ===")
                println("Fehler bei der Abfrage: ${e.message}")
            }
        }
        // fetch names for locations
        val test = ReverseGeoCodingService.updateLocationNames().unsafeRunSync(env)
    }

    private fun printStationInfo(id: Int, thingClean: ThingClean, test: Any) {
        println("Test: $test")
        println("\n=== Station ${thingClean.name} (ID: $id) ===")

        val tideMeasurement = formatTideMeasurement(thingClean)
        println(tideMeasurement)
        println("Position: ${thingClean.location}")
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