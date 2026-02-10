package hs.flensburg.marlin.business.api.apikey.boundary

import hs.flensburg.marlin.business.api.apikey.entity.CreateApiKeyRequest
import hs.flensburg.marlin.business.api.auth.entity.LoggedInUser
import hs.flensburg.marlin.business.api.openAPI.ApiKeyOpenAPISpec
import hs.flensburg.marlin.plugins.Realm
import hs.flensburg.marlin.plugins.authenticate
import hs.flensburg.marlin.plugins.respondKIO
import io.github.smiley4.ktoropenapi.delete
import io.github.smiley4.ktoropenapi.get
import io.github.smiley4.ktoropenapi.post
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respondText
import io.ktor.server.routing.routing
import java.util.UUID

fun Application.configureApiKeyResources() {
    routing {
        authenticate(Realm.COMMON) {
            post("/api-keys", ApiKeyOpenAPISpec.createApiKey) {
                val user = call.principal<LoggedInUser>()!!
                val request = call.receive<CreateApiKeyRequest>()

                call.respondKIO(
                    ApiKeyService.createApiKey(user.id, request)
                )
            }

            get("/api-keys", ApiKeyOpenAPISpec.listApiKeys) {
                val user = call.principal<LoggedInUser>()!!

                call.respondKIO(
                    ApiKeyService.listApiKeys(user.id)
                )
            }

            delete("/api-keys/{id}", ApiKeyOpenAPISpec.revokeApiKey) {
                val user = call.principal<LoggedInUser>()!!
                val keyId = try {
                    UUID.fromString(call.parameters["id"]!!)
                } catch (e: Exception) {
                    return@delete call.respondText("Invalid key ID", status = HttpStatusCode.BadRequest)
                }

                call.respondKIO(
                    ApiKeyService.revokeApiKey(user.id, keyId)
                )
            }
        }
    }
}
