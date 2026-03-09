package hs.flensburg.marlin.business.api.userDevice.boundary

import hs.flensburg.marlin.business.api.openAPI.UserDeviceOpenAPISpec
import hs.flensburg.marlin.business.api.userDevice.entity.CreateUserDeviceRequest
import hs.flensburg.marlin.plugins.respondKIO
import io.github.smiley4.ktoropenapi.delete
import io.github.smiley4.ktoropenapi.get
import io.github.smiley4.ktoropenapi.post
import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.routing.routing

fun Application.configureUserDevices() {
    routing {
        get("/user-device/{id}", UserDeviceOpenAPISpec.getUserDevice) {
            val id = call.parameters["id"]!!.toLong()
            call.respondKIO(UserDeviceService.getUserDevice(id))
        }

        get("/user-device/all/{userId}", UserDeviceOpenAPISpec.getAllUserDevices) {
            val userId = call.parameters["userId"]!!.toLong()
            call.respondKIO(UserDeviceService.getAllUserDevices(userId))
        }

        post("/user-device", UserDeviceOpenAPISpec.postUserDevice) {
            val request = call.receive<CreateUserDeviceRequest>()
            call.respondKIO(UserDeviceService.createDevice(request.userId, request))
        }

        delete("/user-device/{id}", UserDeviceOpenAPISpec.deleteUserDevice) {
            val id = call.parameters["id"]!!.toLong()
            call.respondKIO(UserDeviceService.deleteUserDevice(id))
        }
    }
}