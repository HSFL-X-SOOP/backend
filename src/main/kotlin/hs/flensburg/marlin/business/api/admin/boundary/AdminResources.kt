package hs.flensburg.marlin.business.api.admin.boundary

import com.sun.beans.introspect.PropertyInfo
import hs.flensburg.marlin.business.PageResult
import hs.flensburg.marlin.business.api.admin.entity.Dashboard
import hs.flensburg.marlin.plugins.Realm
import hs.flensburg.marlin.plugins.authenticate
import hs.flensburg.marlin.plugins.respondKIO
import io.ktor.server.application.Application
import io.ktor.server.routing.get
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
                            body<PageResult<Dashboard.DashboardInfo>>()
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