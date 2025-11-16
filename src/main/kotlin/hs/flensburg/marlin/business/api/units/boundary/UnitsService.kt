package hs.flensburg.marlin.business.api.units.boundary

import hs.flensburg.marlin.business.api.sensors.entity.raw.MeasurementTypeDTO
import hs.flensburg.marlin.business.api.units.entity.ConvertedValueDTO
import kotlin.math.PI

object UnitsService {

    val measurementNameMap: Map<String, String> = mapOf(
        "Temperature, water" to "waterTemperature",
        "Wave Height" to "waveHeight",
        "Tide" to "tide",
        "Standard deviation" to "standardDeviation",
        "Battery, voltage" to "batteryVoltage",
        "Temperature, air" to "airTemperature",
        "Wind speed" to "windSpeed",
        "Wind direction" to "windDirection",
        "Wind speed, gust" to "gustSpeed",
        "Wind direction, gust" to "gustDirection",
        "Humidity, relative" to "humidity",
        "Station pressure" to "airPressure"
    )

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
            "m/s" -> performConversion(value, currentType, "km/h")
            "Cel" -> ConvertedValueDTO(value, "°C")
            else -> ConvertedValueDTO(value, currentType)
        }
    }

    private fun mapImperial(value: Double, currentType: String): ConvertedValueDTO {
        val targetUnit: String = when (currentType) {
            "Cel", "°C" -> "°F"
            "m/s" -> "mph"
            "hPa" -> "inHg"
            "cm" -> "in"
            else -> return ConvertedValueDTO(value, currentType)
        }

        return performConversion(value, currentType, targetUnit)
    }

    private fun mapShipping(value: Double, currentType: String): ConvertedValueDTO {
        val targetUnit: String = when (currentType) {
            "m/s" -> "kn"
            "Cel" -> return ConvertedValueDTO(value, "°C")
            else -> return ConvertedValueDTO(value, currentType)
        }

        return performConversion(value, currentType, targetUnit)
    }

    private fun mapCustom(value: Double, currentType: MeasurementTypeDTO, goal: String): ConvertedValueDTO {
        // Regex, um 'key: "value"' Paare zu finden (z.B. waterTemperature: "°F")
        // [a-zA-Z]+ : der Messwert-Name (Gruppe 1)
        // [^,"']+|[^,"']+ : der Einheit-Symbol (Gruppe 2)
        //\s*([a-zA-Z]+)\s*:\s*["']?([^"']+)["']?,?\s*
        val unitRegex = """([^:,]+):([^,]+)""".toRegex()

        val customUnits: Map<String, String> = unitRegex.findAll(goal).associate { matchResult ->
            val key = matchResult.groups[1]!!.value.trim()
            val value = matchResult.groups[2]!!.value.trim()
            key to value
        }
        println(currentType.name)
        println(currentType.unitSymbol)
        val goalUnitSymbol = customUnits[measurementNameMap[currentType.name]]

        return if (goalUnitSymbol != null) {

            val convertedValue = performConversion(
                value, currentType.unitSymbol!!, goalUnitSymbol
            )

            convertedValue

        } else {
            ConvertedValueDTO(
                value = value, unit = currentType.unitSymbol!!
            )
        }
    }

    private fun performConversion(
        value: Double, sourceUnit: String, targetUnit: String
    ): ConvertedValueDTO {

        var sourceUnit = sourceUnit

        if (sourceUnit == "Cel") sourceUnit = "°C"

        if (sourceUnit == targetUnit) return ConvertedValueDTO(value, sourceUnit)

        print(sourceUnit)

        var convertedValue: Double

        when (sourceUnit to targetUnit) {
            // temperature
            "°C" to "°F", "Cel" to "°F" -> convertedValue = value * 9 / 5 + 32
            "°C" to "K"-> convertedValue = value + 273.15

            // speed
            "m/s" to "km/h" -> convertedValue = value * 3.6
            "m/s" to "mph" -> convertedValue = value / 0.44704
            "m/s" to "kn" -> convertedValue = value * 1.943844
            "m/s" to "Bft" -> convertedValue = when {
                value < 0.3 -> 0.0
                value < 1.5 -> 1.0
                value < 3.3 -> 2.0
                value < 5.5 -> 3.0
                value < 8.0 -> 4.0
                value < 10.8 -> 5.0
                value < 13.9 -> 6.0
                value < 17.2 -> 7.0
                value < 20.7 -> 8.0
                value < 24.4 -> 9.0
                value < 28.5 -> 10.0
                value < 32.6 -> 11.0
                else -> 12.0
            }

            // direction
            "deg" to "rad" -> convertedValue = value * PI/180

            // pressure
            "hPa" to "inHg" -> convertedValue = value * 0.02953
            "hPa" to "mbar" -> convertedValue = value
            "hPa" to "psi" -> convertedValue = value * 0.0145037738

            // length
            "cm" to "in" -> convertedValue = value / 2.54
            "cm" to "m" -> convertedValue = value / 100

            else -> return ConvertedValueDTO(value, sourceUnit)
        }

        return ConvertedValueDTO(convertedValue, targetUnit)
    }
}