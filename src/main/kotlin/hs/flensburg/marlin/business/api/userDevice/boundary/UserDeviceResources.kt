package hs.flensburg.marlin.business.api.userDevice.boundary

import hs.flensburg.marlin.business.api.auth.entity.LoggedInUser
import hs.flensburg.marlin.business.api.userDevice.entity.CreateUserDeviceRequest
import hs.flensburg.marlin.business.api.userDevice.entity.UserDevice
import hs.flensburg.marlin.business.api.users.boundary.UserService
import hs.flensburg.marlin.business.api.users.entity.CreateUserProfileRequest
import hs.flensburg.marlin.business.api.users.entity.UserProfile
import hs.flensburg.marlin.plugins.respondKIO
import io.github.smiley4.ktoropenapi.delete
import io.github.smiley4.ktoropenapi.get
import io.github.smiley4.ktoropenapi.post
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.routing.routing
import kotlin.text.toLong

fun Application.configureUserDevices() {
    routing {
        get(
            path = "/user-device/{id}",
            builder = {
                description = "Get a user device ID "
                tags("user-device")
                request {
                    pathParameter<Long>("id") {
                        description = "ID of the user device"
                    }
                }
                response {
                    HttpStatusCode.OK to {
                        body<UserDevice>()
                    }
                    HttpStatusCode.NotFound to {
                        body<String>()
                    }
                }
            }
        ) {
            val id = call.parameters["id"]!!.toLong()
            call.respondKIO(UserDeviceService.getUserDevice(id))
        }

        post(
            path = "/user-device",
            builder = {
                description = "Create a user device entry"
                tags("user-device")
                request {
                    body<CreateUserDeviceRequest>()
                }
                response {
                    HttpStatusCode.Created to {
                        body<UserDevice>()
                    }
                    HttpStatusCode.BadRequest to {
                        body<String>()
                    }
                }
            }
        ) {
            val request = call.receive<CreateUserDeviceRequest>()
            call.respondKIO(UserDeviceService.createDevice(request.userId,request))
        }

        delete(
            path = "/user-device/{id}",
            builder = {
                description = "Delete a user's device by ID."
                tags("user-device")
                request {
                    pathParameter<Long>("id") {
                        description = "ID of the user device entry"
                    }
                }
                response {
                    HttpStatusCode.NoContent to {}
                    HttpStatusCode.NotFound to {
                        body<String>()
                    }
                }
            }
        ) {
            val id = call.parameters["id"]!!.toLong()
            call.respondKIO(UserDeviceService.deleteUserDevice(id))
        }

        /* TODO
        delete(
            path = "/user-devices/user/{userId}",
            builder = {
                description = "Delete all user devices by userId."
                tags("user-device")
                request {
                    pathParameter<Long>("userId") {
                        description = "userId of the user"
                    }
                }
                response {
                    HttpStatusCode.NoContent to {}
                    HttpStatusCode.NotFound to {
                        body<String>()
                    }
                }
            }
        ) {
            val userId = call.parameters["userId"]!!.toLong()
            call.respondKIO(UserDeviceService.deleteAllUserDevice(userId))
        }
        */
    }
}