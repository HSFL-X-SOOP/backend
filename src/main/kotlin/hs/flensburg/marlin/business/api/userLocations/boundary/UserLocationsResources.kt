import hs.flensburg.marlin.business.api.openAPI.UserLocationsOpenAPISpec
import hs.flensburg.marlin.business.api.userLocations.entity.CreateOrUpdateUserLocationRequest
import hs.flensburg.marlin.plugins.respondKIO
import io.github.smiley4.ktoropenapi.delete
import io.github.smiley4.ktoropenapi.get
import io.github.smiley4.ktoropenapi.post
import io.github.smiley4.ktoropenapi.put
import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.routing.routing

fun Application.configureUserLocations() {
    routing {
        get("/user-locations/{id}", UserLocationsOpenAPISpec.getUserLocation) {
            val id = call.parameters["id"]!!.toLong()
            call.respondKIO(UserLocationsService.getUserLocation(id))
        }

        get(
            "/user-locations/{userId}/{locationId}",
            UserLocationsOpenAPISpec.getUserLocationUserLocation
        ) {
            val userId = call.parameters["userId"]!!.toLong()
            val locationId = call.parameters["locationId"]!!.toLong()
            call.respondKIO(UserLocationsService.getUserLocationByUserIdAndLocationId(userId, locationId))
        }

        get("/user-locations/user/{userId}", UserLocationsOpenAPISpec.getAllUserLocationsUser) {
            val userId = call.parameters["userId"]!!.toLong()
            call.respondKIO(UserLocationsService.getAllUserLocationsFromUser(userId))
        }

        post("/user-locations", UserLocationsOpenAPISpec.postUserLocation) {
            val request = call.receive<CreateOrUpdateUserLocationRequest>()
            call.respondKIO(UserLocationsService.create(request))
        }

        put("/user-locations/{id}", UserLocationsOpenAPISpec.putUserLocation) {
            val id = call.parameters["id"]!!.toLong()
            val request = call.receive<CreateOrUpdateUserLocationRequest>()
            call.respondKIO(UserLocationsService.update(id, request))
        }

        delete("/user-locations/{id}", UserLocationsOpenAPISpec.deleteUserLocation) {
            val id = call.parameters["id"]!!.toLong()
            call.respondKIO(UserLocationsService.delete(id))
        }
    }
}