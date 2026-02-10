package hs.flensburg.marlin.business.api.openAPI

import hs.flensburg.marlin.business.api.apikey.entity.ApiKeyInfo
import hs.flensburg.marlin.business.api.apikey.entity.CreateApiKeyRequest
import hs.flensburg.marlin.business.api.apikey.entity.CreateApiKeyResponse
import hs.flensburg.marlin.business.api.apikey.entity.RevokeApiKeyResponse
import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.ktor.http.HttpStatusCode

object ApiKeyOpenAPISpec {

    val createApiKey: RouteConfig.() -> Unit = {
        tags("api-keys")
        description = "Create a new API key for external API access. " +
                "Requires an active API_ACCESS subscription. " +
                "Any existing active key will be revoked. " +
                "The full key is only returned once in the response."
        securitySchemeNames("BearerAuth")

        request {
            body<CreateApiKeyRequest> {
                description = "Optional name for the API key"
            }
        }

        response {
            HttpStatusCode.OK to {
                description = "API key created successfully. Save the key - it won't be shown again."
                body<CreateApiKeyResponse>()
            }
            HttpStatusCode.Forbidden to {
                description = "Active API_ACCESS subscription required"
                body<String>()
            }
            HttpStatusCode.Unauthorized to {
                description = "User not authenticated"
                body<String>()
            }
        }
    }

    val listApiKeys: RouteConfig.() -> Unit = {
        tags("api-keys")
        description = "List all API keys for the authenticated user. " +
                "Returns key prefix and metadata only (never the full key or hash)."
        securitySchemeNames("BearerAuth")

        response {
            HttpStatusCode.OK to {
                description = "List of API keys"
                body<List<ApiKeyInfo>>()
            }
            HttpStatusCode.Unauthorized to {
                description = "User not authenticated"
                body<String>()
            }
        }
    }

    val revokeApiKey: RouteConfig.() -> Unit = {
        tags("api-keys")
        description = "Revoke an API key by its ID. The key will no longer be usable for authentication."
        securitySchemeNames("BearerAuth")

        request {
            pathParameter<String>("id") {
                description = "UUID of the API key to revoke"
            }
        }

        response {
            HttpStatusCode.OK to {
                description = "API key revoked successfully"
                body<RevokeApiKeyResponse>()
            }
            HttpStatusCode.NotFound to {
                description = "API key not found or does not belong to user"
                body<String>()
            }
            HttpStatusCode.Unauthorized to {
                description = "User not authenticated"
                body<String>()
            }
        }
    }
}
