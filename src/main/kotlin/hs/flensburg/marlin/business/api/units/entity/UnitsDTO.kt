package hs.flensburg.marlin.business.api.units.entity

import kotlinx.serialization.Serializable

@Serializable
data class UnitsDTO (
    val waterTemperature: String,
    val waveHeight: String,
    val tide: String,
    val standardDeviation: String,
    val batteryVoltage: String,
    val airTemperature: String,
    val windSpeed: String,
    val windDirection: String,
    val gustSpeed: String,
    val gustDirection: String,
    val humidity: String,
    val airPressure: String
)