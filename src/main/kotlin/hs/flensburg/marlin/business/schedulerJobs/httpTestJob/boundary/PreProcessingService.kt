package hs.flensburg.marlin.business.schedulerJobs.httpTestJob.boundary

import hs.flensburg.marlin.business.schedulerJobs.httpTestJob.entity.ThingClean
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import java.nio.file.Path
import java.nio.file.Paths

object PreProcessingService {
    fun preProcessData(thing: ThingClean): ThingClean {
        // read config
        val config = readConfig()

        // preprocess datastream
        val newDatastreams = thing.datastreams.map { ds ->
            val processedName = config[ds.observedProperty.name] ?: ds.observedProperty.name

            // create copy to change the name of the observed property
            ds.copy(
                observedProperty = ds.observedProperty.copy(
                    name = processedName
                )
            )
        }
        // return preprocessed data
        return thing.copy(datastreams = newDatastreams)
    }
   private fun readConfig(): Map<String, String> {
       val resource = {}.javaClass.getResource("/SensorPreProcessing/PreProcessConfig.json")
           ?: throw IllegalStateException("PreProcessConfig.json not found!")

       val text = resource.readText()
       // build Map from JSON to swap values
       val json = Json.decodeFromString<Map<String, String>>(text)
       return json
    }
}