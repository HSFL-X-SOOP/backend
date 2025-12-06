package hs.flensburg.marlin.business.api.units.boundary

import hs.flensburg.marlin.business.api.units.entity.ConvertedValueDTO
import kotlin.math.PI

object UnitsService {

    const val CELSIUS_TO_FAHRENHEIT_FACTOR = 9 / 5 + 32
    const val CELSIUS_TO_KELVIN_SUMMAND = 273.15
    const val MS_TO_KMH_FACTOR = 3.6
    const val MS_TO_MPH_DIVISOR = 0.44704
    const val MS_TO_KNOTS_FACTOR = 1.943844
    const val DEGREES_TO_RADIANS_FACTOR = PI/180
    const val HECTOPASCAL_TO_INCH_OF_MERCURY_FACTOR = 0.02953
    const val HECTOPASCAL_TO_POUND_PER_SQUARE_INCH_FACTOR = 0.0145037738
    const val CENTIMETER_TO_INCHES_DEVISOR = 2.54
    const val CENTIMETER_TO_METER_DEVISOR = 100

    const val METERS_PER_SECOND_BEAUFORT_BORDER_0 = 0.3
    const val METERS_PER_SECOND_BEAUFORT_BORDER_1 = 1.5
    const val METERS_PER_SECOND_BEAUFORT_BORDER_2 = 3.3
    const val METERS_PER_SECOND_BEAUFORT_BORDER_3 = 5.5
    const val METERS_PER_SECOND_BEAUFORT_BORDER_4 = 8.0
    const val METERS_PER_SECOND_BEAUFORT_BORDER_5 = 10.8
    const val METERS_PER_SECOND_BEAUFORT_BORDER_6 = 13.9
    const val METERS_PER_SECOND_BEAUFORT_BORDER_7 = 17.2
    const val METERS_PER_SECOND_BEAUFORT_BORDER_8 = 20.7
    const val METERS_PER_SECOND_BEAUFORT_BORDER_9 = 24.4
    const val METERS_PER_SECOND_BEAUFORT_BORDER_10 = 28.5
    const val METERS_PER_SECOND_BEAUFORT_BORDER_11 = 32.6

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

    fun convert(value: Double, measurementName: String, unitSymbol: String, goal: String): ConvertedValueDTO {
        return when (goal) {
            "", "metric" -> mapMetric(value, unitSymbol)
            "imperial" -> mapImperial(value, unitSymbol)
            "shipping" -> mapShipping(value, unitSymbol)
            else -> mapCustom(value, measurementName, unitSymbol, goal)
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

    private fun mapCustom(value: Double, measurementName: String, unitSymbol: String, goal: String): ConvertedValueDTO {
        // Regex for finding 'key: "value"' pairs (i.e. waterTemperature: "°F")
        val unitRegex = """([^:,]+):([^,]+)""".toRegex()

        val customUnits: Map<String, String> = unitRegex.findAll(goal).associate { matchResult ->
            val key = matchResult.groups[1]!!.value.trim()
            val value = matchResult.groups[2]!!.value.trim()
            key to value
        }
        val goalUnitSymbol = customUnits[measurementNameMap[measurementName]]

        return if (goalUnitSymbol != null) {

            val convertedValue = performConversion(
                value, unitSymbol, goalUnitSymbol
            )

            convertedValue

        } else {
            ConvertedValueDTO(
                value = value, unit = unitSymbol
            )
        }
    }

    private fun performConversion(
        value: Double, sourceUnit: String, targetUnit: String
    ): ConvertedValueDTO {

        var sourceUnit = sourceUnit

        if (sourceUnit == "Cel") sourceUnit = "°C"

        if (sourceUnit == targetUnit) return ConvertedValueDTO(value, sourceUnit)

        var convertedValue: Double

        when (sourceUnit to targetUnit) {
            // temperature
            "°C" to "°F", "Cel" to "°F" -> convertedValue = value * CELSIUS_TO_FAHRENHEIT_FACTOR
            "°C" to "K"-> convertedValue = value + CELSIUS_TO_KELVIN_SUMMAND

            // speed
            "m/s" to "km/h" -> convertedValue = value * MS_TO_KMH_FACTOR
            "m/s" to "mph" -> convertedValue = value / MS_TO_MPH_DIVISOR
            "m/s" to "kn" -> convertedValue = value * MS_TO_KNOTS_FACTOR

            "m/s" to "Bft" -> convertedValue = when {
                value < METERS_PER_SECOND_BEAUFORT_BORDER_0 -> 0.0
                value < METERS_PER_SECOND_BEAUFORT_BORDER_1 -> 1.0
                value < METERS_PER_SECOND_BEAUFORT_BORDER_2 -> 2.0
                value < METERS_PER_SECOND_BEAUFORT_BORDER_3 -> 3.0
                value < METERS_PER_SECOND_BEAUFORT_BORDER_4 -> 4.0
                value < METERS_PER_SECOND_BEAUFORT_BORDER_5 -> 5.0
                value < METERS_PER_SECOND_BEAUFORT_BORDER_6 -> 6.0
                value < METERS_PER_SECOND_BEAUFORT_BORDER_7 -> 7.0
                value < METERS_PER_SECOND_BEAUFORT_BORDER_8 -> 8.0
                value < METERS_PER_SECOND_BEAUFORT_BORDER_9 -> 9.0
                value < METERS_PER_SECOND_BEAUFORT_BORDER_10 -> 10.0
                value < METERS_PER_SECOND_BEAUFORT_BORDER_11 -> 11.0
                else -> 12.0
            }

            // direction
            "deg" to "rad" -> convertedValue = value * DEGREES_TO_RADIANS_FACTOR

            // pressure
            "hPa" to "inHg" -> convertedValue = value * HECTOPASCAL_TO_INCH_OF_MERCURY_FACTOR
            "hPa" to "mbar" -> convertedValue = value
            "hPa" to "psi" -> convertedValue = value * HECTOPASCAL_TO_POUND_PER_SQUARE_INCH_FACTOR

            // length
            "cm" to "in" -> convertedValue = value / CENTIMETER_TO_INCHES_DEVISOR
            "cm" to "m" -> convertedValue = value / CENTIMETER_TO_METER_DEVISOR

            else -> return ConvertedValueDTO(value, sourceUnit)
        }

        return ConvertedValueDTO(convertedValue, targetUnit)
    }
}