package hs.flensburg.marlin.business.schedulerJobs.sensorData.boundary

import hs.flensburg.marlin.business.schedulerJobs.sensorData.entity.ThingClean
import kotlinx.serialization.json.Json
import java.util.Collections.emptyMap

object PreProcessingService {

    fun preProcessData(thing: ThingClean): ThingClean {
        // read config from json file
        val config = readConfig()

        // Create mappings for name and description
        val nameMapping = config["name"] ?: emptyMap()
        val descriptionMapping = config["description"] ?: emptyMap()

        val newDatastreams = thing.datastreams.map { ds ->
            // Preprocess the name by looking up in the mapping
            val processedName = nameMapping[ds.observedProperty.name] ?: ds.observedProperty.name

            // Optimize description by matching part of the string
            val processedDesc = descriptionMapping.entries
                .firstOrNull { ds.observedProperty.description.contains(it.key, ignoreCase = true) }
                ?.value ?: ds.observedProperty.description

            // Return a copy with porcessed name and description
            ds.copy(
                observedProperty = ds.observedProperty.copy(name = processedName, description = processedDesc)
            )
        }

        // Return a new ThingClean with preprocessed datastreams
        return thing.copy(datastreams = newDatastreams)
    }

    private fun readConfig(): Map<String, Map<String, String>> {
        // Load the JSON configuration file from resources
        val resource = {}.javaClass.getResource("/SensorPreProcessing/PreProcessConfig.json")
            ?: throw IllegalStateException("PreProcessConfig.json not found!")

        val text = resource.readText()
        // Parse JSON into a nested Map: field -> mapping
        return Json.decodeFromString(text)
    }
}
