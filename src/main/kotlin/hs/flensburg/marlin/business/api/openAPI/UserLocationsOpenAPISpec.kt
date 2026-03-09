package hs.flensburg.marlin.business.api.openAPI

import UserLocationDTO
import hs.flensburg.marlin.business.api.userLocations.entity.CreateOrUpdateUserLocationRequest
import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.ktor.http.HttpStatusCode

object UserLocationsOpenAPISpec {

    val getUserLocation: RouteConfig.() -> Unit = {
        description = "Get a user location by its ID "
        tags("user-locations")
        request {
            pathParameter<Long>("id") {
                description = "ID of the user location"
            }
        }
        response {
            HttpStatusCode.OK to {
                body<UserLocationDTO>()
            }
            HttpStatusCode.NotFound to {
                body<String>()
            }
        }
    }

    val getUserLocationUserLocation: RouteConfig.() -> Unit = {
        description = "Get a user location by its user ID and location ID"
        tags("user-locations")
        request {
            pathParameter<Long>("userId") {
                description = "ID of the user"
            }
            pathParameter<Long>("locationId") {
                description = "ID of the location"
            }
        }
        response {
            HttpStatusCode.OK to {
                body<UserLocationDTO>()
            }
            HttpStatusCode.NotFound to {
                body<String>()
            }
        }
    }

    val getAllUserLocationsUser: RouteConfig.() -> Unit = {
        description = "Get all user locations from a user by the user ID"
        tags("user-locations")
        request {
            pathParameter<Long>("userId") {
                description = "ID of the user"
            }
        }
        response {
            HttpStatusCode.OK to {
                body<UserLocationDTO>()
            }
            HttpStatusCode.NotFound to {
                body<String>()
            }
        }
    }

    val postUserLocation: RouteConfig.() -> Unit = {
        description = "Create a user location"
        tags("user-locations")
        request {
            body<CreateOrUpdateUserLocationRequest>()
        }
        response {
            HttpStatusCode.Created to {
                body<UserLocationDTO>()
            }
            HttpStatusCode.BadRequest to {
                body<String>()
            }
        }
    }

    val putUserLocation: RouteConfig.() -> Unit = {
        description = "Update a user location by its ID"
        tags("user-locations")
        request {
            body<CreateOrUpdateUserLocationRequest>()
        }
        response {
            HttpStatusCode.OK to {
                body<UserLocationDTO>()
            }
            HttpStatusCode.NotFound to {
                body<String>()
            }
        }
    }

    val deleteUserLocation: RouteConfig.() -> Unit = {
        description = "Delete a user location by ID."
        tags("user-locations")
        request {
            pathParameter<Long>("id") {
                description = "ID of the user location"
            }
        }
        response {
            HttpStatusCode.NoContent to {}
            HttpStatusCode.NotFound to {
                body<String>()
            }
        }
    }

}