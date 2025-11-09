package hs.flensburg.marlin.business.api.location.entity

import kotlinx.serialization.Serializable

@Serializable
data class ImageRequest(
    val base64: String? = null,
    val contentType: String? = null
)