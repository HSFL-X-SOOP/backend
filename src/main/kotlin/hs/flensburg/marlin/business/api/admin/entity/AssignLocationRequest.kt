package hs.flensburg.marlin.business.api.admin.entity

import kotlinx.serialization.Serializable

@Serializable
data class AssignLocationRequest(
    val userId: Long,
    val locationId: Long
)
