package hs.flensburg.marlin.business.api.admin.entity

class Dashboard {

    data class DashboardInfo(
        val totalLocations: Int,
        val totalSensors: Int,
        val totalUsers: Int,
        val totalMeasurements: Int
    )
}