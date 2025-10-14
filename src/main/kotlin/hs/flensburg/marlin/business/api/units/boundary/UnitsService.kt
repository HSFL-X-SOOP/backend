package hs.flensburg.marlin.business.api.units.boundary

import hs.flensburg.marlin.business.api.units.entity.ConvertedValueDTO

object UnitsService {
    fun convert(value: Double, currentType: String, goal: String): ConvertedValueDTO {
        return when (goal) {
            "metric" -> mapMetric(value, currentType)
            "imperial" -> mapImperial(value, currentType)
            "shipping" -> mapShipping(value, currentType)
            else -> mapCustom(value, currentType, goal)
        }
    }

    private fun mapMetric(value: Double, currentType: String): ConvertedValueDTO {
        return when (currentType) {
            "m/s" -> ConvertedValueDTO(value * 3.6, "km/h")
            else -> ConvertedValueDTO(value, currentType)
        }
    }

    private fun mapImperial(value: Double, currentType: String): ConvertedValueDTO {
        return when (currentType) {
            "Cel" -> ConvertedValueDTO(value * 9/5 + 32, "°F")
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

    private fun mapCustom(value: Double, currentType: String, goal: String): ConvertedValueDTO {
        return ConvertedValueDTO(value, currentType)
    }
}