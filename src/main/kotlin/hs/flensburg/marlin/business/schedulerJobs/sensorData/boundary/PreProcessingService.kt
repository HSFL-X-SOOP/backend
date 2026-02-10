package hs.flensburg.marlin.business.schedulerJobs.sensorData.boundary

import hs.flensburg.marlin.business.schedulerJobs.sensorData.entity.ThingClean
import kotlinx.serialization.json.*

object PreProcessingService {

    fun preProcessData(thing: ThingClean): ThingClean {
        val config = readConfig()

        val processedLocation = getManualLocation(config, thing.id.toString()) ?: thing.location
        val nameMap = getStringMap(config, "name")
        val descMap = getStringMap(config, "description")

        val newDatastreams = thing.datastreams.map { ds ->
            val processedName = nameMap[ds.observedProperty.name] ?: ds.observedProperty.name

            val processedDesc = descMap.entries
                .firstOrNull { (key, _) -> ds.observedProperty.description.contains(key, ignoreCase = true) }
                ?.value ?: ds.observedProperty.description

            ds.copy(observedProperty = ds.observedProperty.copy(name = processedName, description = processedDesc))
        }

        return thing.copy(location = processedLocation, datastreams = newDatastreams)
    }

    private fun readConfig(): JsonObject {
        val text = {}.javaClass.getResource("/SensorPreProcessing/PreProcessConfig.json")?.readText()
            ?: throw IllegalStateException("Config not found!")
        return Json.parseToJsonElement(text).jsonObject
    }

    private fun getStringMap(config: JsonObject, key: String): Map<String, String> {
        return config[key]?.jsonObject?.mapValues { it.value.jsonPrimitive.content } ?: emptyMap()
    }

    private fun getManualLocation(config: JsonObject, sensorId: String): Pair<Double, Double>? {
        val coords = config["location"]?.jsonObject?.get(sensorId)?.jsonArray ?: return null
        return if (coords.size >= 2) {
            coords[0].jsonPrimitive.double to coords[1].jsonPrimitive.double
        } else null
    }
}
