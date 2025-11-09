package hs.flensburg.marlin.business.api.admin.entity

import kotlinx.serialization.Serializable


@Serializable
data class DashboardInfo(
    val totalLocations: Int,
    val totalSensors: Int,
    val totalUsers: Int,
    val totalMeasurements: Int
)
