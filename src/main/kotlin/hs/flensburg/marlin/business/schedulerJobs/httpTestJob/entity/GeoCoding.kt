package hs.flensburg.marlin.business.schedulerJobs.httpTestJob.entity

import kotlinx.serialization.Serializable

@Serializable
data class NominatimResponse(
    val display_name: String? = null,
    val error: String? = null
)
