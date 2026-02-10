package hs.flensburg.marlin.business.api.apikey.entity

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class CreateApiKeyRequest(
    val name: String? = null
)

@Serializable
data class CreateApiKeyResponse(
    val id: String,
    val key: String,
    val prefix: String,
    val name: String?,
    val createdAt: String
)

@Serializable
data class ApiKeyInfo(
    val id: String,
    val prefix: String,
    val name: String?,
    val isActive: Boolean,
    val lastUsedAt: String?,
    val createdAt: String
)

@Serializable
data class RevokeApiKeyResponse(
    val revoked: Boolean
)
