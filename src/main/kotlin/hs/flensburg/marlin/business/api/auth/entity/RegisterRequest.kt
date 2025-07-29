package hs.flensburg.marlin.business.api.auth.entity

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(val email: String, val password: String, val rememberMe: Boolean)