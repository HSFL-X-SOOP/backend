package hs.flensburg.marlin.business.api.admin.boundary

import hs.flensburg.marlin.business.api.admin.entity.DashboardInfo
import hs.flensburg.marlin.plugins.Realm
import hs.flensburg.marlin.plugins.authenticate
import hs.flensburg.marlin.plugins.respondKIO
import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import io.github.smiley4.ktoropenapi.get
import io.ktor.http.HttpStatusCode

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
        }
    }
}