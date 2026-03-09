package hs.flensburg.marlin.business.api.admin.boundary

import hs.flensburg.marlin.business.api.admin.entity.AssignLocationRequest
import hs.flensburg.marlin.business.api.auth.entity.LoggedInUser
import hs.flensburg.marlin.business.api.openAPI.AdminOpenAPISpec
import hs.flensburg.marlin.plugins.Realm
import hs.flensburg.marlin.plugins.authenticate
import hs.flensburg.marlin.plugins.respondKIO
import io.github.smiley4.ktoropenapi.get
import io.github.smiley4.ktoropenapi.post
import io.ktor.server.application.Application
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.routing.routing

fun Application.configureAdmin() {
    routing {
        authenticate(Realm.ADMIN) {
            get("/admin/dashboardInfo", AdminOpenAPISpec.getAdminDashboardInfo) {
                call.respondKIO(
                    AdminService.getDashboardInformation()
                )
            }

            post("/admin/assignLocation", AdminOpenAPISpec.postAdminAssignLocation) {
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
        }
    }
}