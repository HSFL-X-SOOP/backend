package hs.flensburg.marlin.business.api.auth.entity

import kotlinx.serialization.Serializable

@Serializable
data class ApplePublicKeysResponse(
    val keys: List<ApplePublicKey>
)