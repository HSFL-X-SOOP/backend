package hs.flensburg.marlin.business.api.auth.entity

import kotlinx.serialization.Serializable

@Serializable
data class LoggedInUser(val id: Long, val email: String)