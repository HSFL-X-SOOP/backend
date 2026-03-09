package hs.flensburg.marlin.business.api.openAPI

import hs.flensburg.marlin.business.api.potentialSensors.entity.PotentialSensorDTO
import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.ktor.http.HttpStatusCode

object PotentialSensorsOpenAPISpec {

    val getPotentialSensors: RouteConfig.() -> Unit = {
        description = "Get all potential sensors. Requires admin role."
        tags("admin", "potential-sensors")
        securitySchemeNames("BearerAuthAdmin")
        description = "Get all potential sensors"
        tags("admin")
        response {
            HttpStatusCode.OK to {
                description = "List of potential sensors"
                body<List<PotentialSensorDTO>>()
            }
            HttpStatusCode.Unauthorized to {
                description = "Missing or invalid JWT token, or insufficient permissions (admin role required)"
                body<String>()
            }
            HttpStatusCode.InternalServerError to {
                description = "Error retrieving potential sensors"
            }
        }
    }

    val getPotentialSensorToggle: RouteConfig.() -> Unit = {
        description = "Toggle active state of potential sensors. Requires admin role."
        tags("admin")
        securitySchemeNames("BearerAuthAdmin")
        request {
            pathParameter<Long>("id") {
                description = "The sensor ID"
            }
        }
        response {
            HttpStatusCode.OK to {
                description = "potential sensors with updated active state"
                body<List<PotentialSensorDTO>>()
            }
            HttpStatusCode.BadRequest to {
                description = "Invalid sensor ID"
                body<String>()
            }
            HttpStatusCode.Unauthorized to {
                description = "Missing or invalid JWT token, or insufficient permissions (admin role required)"
                body<String>()
            }
            HttpStatusCode.InternalServerError to {
                description = "Error retrieving potential sensors"
            }
        }
    }

}