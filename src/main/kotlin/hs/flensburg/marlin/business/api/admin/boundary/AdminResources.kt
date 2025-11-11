package hs.flensburg.marlin.business.api.admin.boundary

import hs.flensburg.marlin.business.api.admin.entity.AssignLocationRequest
import hs.flensburg.marlin.business.api.admin.entity.ChangeUserRoleRequest
import hs.flensburg.marlin.business.api.admin.entity.DashboardInfo
import hs.flensburg.marlin.business.api.auth.entity.LoggedInUser
import hs.flensburg.marlin.plugins.Realm
import hs.flensburg.marlin.plugins.authenticate
import hs.flensburg.marlin.plugins.respondKIO
import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.routing.routing
import io.github.smiley4.ktoropenapi.get
import io.github.smiley4.ktoropenapi.post
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.principal

fun Application.configureAdmin() {
    routing {
        authenticate(Realm.ADMIN) {
            get(
                path = "/admin/dashboardInfo",
                builder = {
                    description = "Get dashboard information (Admin only)"
                    tags("admin")
                    response {
                        HttpStatusCode.OK to {
                            body<DashboardInfo>()
                        }
                    }
                }

            ) {
                call.respondKIO(
                    AdminService.getDashboardInformation()
                )
            }

            post(
                path = "/admin/assignLocation",
                builder = {
                    description = "Assign a location to a harbor master (Admin only)"
                    tags("admin")
                    request {
                        body<AssignLocationRequest>()
                    }
                    response {
                        HttpStatusCode.OK to {
                            description = "Location successfully assigned to harbor master"
                        }
                        HttpStatusCode.BadRequest to {
                            description = "Invalid request (user is not a harbor master)"
                        }
                        HttpStatusCode.NotFound to {
                            description = "User or location not found"
                        }
                    }
                }
            ) {
                val admin = call.principal<LoggedInUser>()!!
                val request = call.receive<AssignLocationRequest>()

                call.respondKIO(
                    AdminService.assignLocationToHarborMaster(
                        userId = request.userId,
                        locationId = request.locationId,
                        adminId = admin.id
                    )
                )
            }

            post(
                path = "/admin/changeUserRole",
                builder = {
                    description = "Change user role (upgrade to harbor master or downgrade to user) (Admin only)"
                    tags("admin")
                    request {
                        body<ChangeUserRoleRequest>()
                    }
                    response {
                        HttpStatusCode.OK to {
                            description = "User role successfully changed"
                        }
                        HttpStatusCode.NotFound to {
                            description = "User not found"
                        }
                    }
                }
            ) {
                val request = call.receive<ChangeUserRoleRequest>()

                call.respondKIO(
                    AdminService.changeUserRole(
                        userId = request.userId,
                        newRole = request.newRole
                    )
                )
            }
        }
    }
}