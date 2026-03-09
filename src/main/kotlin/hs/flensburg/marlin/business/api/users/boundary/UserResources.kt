package hs.flensburg.marlin.business.api.users.boundary

import hs.flensburg.marlin.business.Page
import hs.flensburg.marlin.business.api.auth.entity.LoggedInUser
import hs.flensburg.marlin.business.api.openAPI.UserOpenAPISpec
import hs.flensburg.marlin.business.api.users.entity.*
import hs.flensburg.marlin.plugins.Realm
import hs.flensburg.marlin.plugins.authenticate
import hs.flensburg.marlin.plugins.respondKIO
import io.github.smiley4.ktoropenapi.delete
import io.github.smiley4.ktoropenapi.get
import io.github.smiley4.ktoropenapi.post
import io.github.smiley4.ktoropenapi.put
import io.ktor.server.application.Application
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.routing.routing

fun Application.configureUsers() {
    routing {
        authenticate(Realm.ADMIN) {
            get("/admin/user-profiles", UserOpenAPISpec.getAllUserProfiles) {
                call.respondKIO(
                    UserService.getProfiles(
                        Page.from(call.request.queryParameters, UserSearchParameters)
                    )
                )
            }

            get(path = "/admin/user-profiles/{userId}", UserOpenAPISpec.getUserProfileById) {
                val userId = call.parameters["userId"]!!.toLong()
                call.respondKIO(UserService.getProfile(userId))
            }

            get(
                "/admin/user-profiles/{userId}/recent-activity",
                UserOpenAPISpec.getUserRecentActivity
            ) {
                val userId = call.parameters["userId"]!!.toLong()
                call.respondKIO(UserService.getRecentActivity(userId))
            }

            post("/admin/user-profiles/block", UserOpenAPISpec.blockUser) {
                val request = call.receive<BlacklistUserRequest>()
                call.respondKIO(UserService.addUserToBlacklist(request))
            }

            put("/admin/user-profiles", UserOpenAPISpec.updateUserProfileAdmin) {
                val request = call.receive<UpdateUserRequest>()
                call.respondKIO(UserService.updateProfile(request))
            }

            delete("/admin/user-profiles/{userId}", UserOpenAPISpec.deleteUserAdmin) {
                val userId = call.parameters["userId"]!!.toLong()
                call.respondKIO(UserService.deleteUser(userId))
            }
        }

        authenticate(Realm.COMMON) {
            get("/user-profile", UserOpenAPISpec.getCurrentUserProfile) {
                val user = call.principal<LoggedInUser>()!!
                call.respondKIO(UserService.getProfile(user.id))
            }

            post("/user-profile", UserOpenAPISpec.createUserProfile) {
                val user = call.principal<LoggedInUser>()!!
                val request = call.receive<CreateUserProfileRequest>()
                call.respondKIO(UserService.createProfile(user.id, request))
            }

            put("/user-profile", UserOpenAPISpec.updateCurrentUserProfile) {
                val user = call.principal<LoggedInUser>()!!
                val request = call.receive<UpdateUserProfileRequest>()
                call.respondKIO(UserService.updateProfile(user.id, request))
            }

            delete("/user-profile", UserOpenAPISpec.deleteCurrentUser) {
                val user = call.principal<LoggedInUser>()!!
                call.respondKIO(UserService.deleteUser(user))
            }
        }
    }
}