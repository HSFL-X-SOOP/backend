package hs.flensburg.marlin.business.api.units.boundary

import hs.flensburg.marlin.business.api.sensors.entity.raw.MeasurementTypeDTO
import hs.flensburg.marlin.business.api.units.entity.ConvertedValueDTO

object UnitsService {
    fun convert(value: Double, currentType: MeasurementTypeDTO, goal: String): ConvertedValueDTO {
        return when (goal) {
            "", "metric" -> mapMetric(value, currentType.unitSymbol!!)
            "imperial" -> mapImperial(value, currentType.unitSymbol!!)
            "shipping" -> mapShipping(value, currentType.unitSymbol!!)
            else -> mapCustom(value, currentType, goal)
        }
    }

    private fun mapMetric(value: Double, currentType: String): ConvertedValueDTO {
        return when (currentType) {
            "m/s" -> ConvertedValueDTO(value * 3.6, "km/h")
            "Cel" -> ConvertedValueDTO(value, "°C")
            else -> ConvertedValueDTO(value, currentType)
        }
    }

    private fun mapImperial(value: Double, currentType: String): ConvertedValueDTO {
        return when (currentType) {
            "Cel" -> ConvertedValueDTO(value * 9/5 + 32, "°F")
            "°C" -> ConvertedValueDTO(value * 9/5 + 32, "°F")
            "m/s" -> ConvertedValueDTO(value * 2.2369362920544025, "mph")
            "hPa" -> ConvertedValueDTO(value * 0.02953, "inHg")
            "cm" -> ConvertedValueDTO(value / 2.54, "in")
            else -> ConvertedValueDTO(value, currentType)
        }
    }

    private fun mapShipping(value: Double, currentType: String): ConvertedValueDTO {
        return when (currentType) {
            "m/s" -> ConvertedValueDTO(value * 1.943844, "kn")
            else -> ConvertedValueDTO(value, currentType)
        }
    }

    private fun mapCustom(value: Double, currentType: MeasurementTypeDTO, goal: String): ConvertedValueDTO {
        // Regex, um 'key: "value"' Paare zu finden (z.B. waterTemperature: "°F")
        // [a-zA-Z]+ : der Messwert-Name (Gruppe 1)
        // [^,"']+|[^,"']+ : der Einheit-Symbol (Gruppe 2)
        val unitRegex = """\s*([a-zA-Z]+)\s*:\s*["']?([^"']+)["']?,?\s*""".toRegex()

        val customUnits: Map<String, String> = goal
            .substringAfter("custom:", "") // "custom:" Präfix entfernen
            .trim()
            .let { remainingString ->
                unitRegex.findAll(remainingString)
                    .associate { matchResult ->
                        val key = matchResult.groups[1]!!.value.trim()
                        val value = matchResult.groups[2]!!.value.trim()
                        key to value
                    }
            }

        val measurementName = currentType.name
        val goalUnitSymbol = customUnits[measurementName]

        return if (goalUnitSymbol != null) {

            val convertedValue = performConversion(
                value,
                currentType.unitSymbol!!,
                goalUnitSymbol
            )

            ConvertedValueDTO(
                value = convertedValue,
                unit = goalUnitSymbol
            )

        } else {
            ConvertedValueDTO(
                value = value,
                unit = currentType.unitSymbol!!
            )
        }
    }

    private fun performConversion(
        value: Double,
        sourceUnit: String,
        targetUnit: String
    ): Double {
        if (sourceUnit == targetUnit) return value

        return when (sourceUnit to targetUnit) {
            "°C" to "°F", "Cel" to "°F" -> value * 9/5 + 32
            "K" to "°C" -> value - 273.15
            "m/s" to "mph" -> value * 2.2369362920544025
            else -> value
        }
    }
}