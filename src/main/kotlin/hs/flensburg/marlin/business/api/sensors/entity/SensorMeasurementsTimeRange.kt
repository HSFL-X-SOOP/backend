package hs.flensburg.marlin.business.api.sensors.entity

enum class SensorMeasurementsTimeRange(val sqlExpression: String) {
    LAST_24_HOURS("24h"),
    LAST_48_HOURS("48h"),
    LAST_7_DAYS("7d"),
    LAST_30_DAYS("30d"),
    LAST_90_DAYS("90d"),
    LAST_180_DAYS("180d"),
    LAST_1_YEAR("1y");

    companion object {
        fun fromString(value: String): SensorMeasurementsTimeRange? {
            return entries.find { it.sqlExpression.equals(value, ignoreCase = true) }
        }
    }
}