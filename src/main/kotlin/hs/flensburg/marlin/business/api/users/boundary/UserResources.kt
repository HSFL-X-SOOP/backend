package hs.flensburg.marlin.business.api.users.boundary

import hs.flensburg.marlin.business.Page
import hs.flensburg.marlin.business.PageResult
import hs.flensburg.marlin.business.api.auth.entity.LoggedInUser
import hs.flensburg.marlin.business.api.users.entity.BlacklistUserRequest
import hs.flensburg.marlin.business.api.users.entity.CreateUserProfileRequest
import hs.flensburg.marlin.business.api.users.entity.UpdateUserProfileRequest
import hs.flensburg.marlin.business.api.users.entity.UpdateUserRequest
import hs.flensburg.marlin.business.api.users.entity.UserProfile
import hs.flensburg.marlin.business.api.users.entity.UserSearchParameters
import hs.flensburg.marlin.plugins.Realm
import hs.flensburg.marlin.plugins.authenticate
import hs.flensburg.marlin.plugins.respondKIO
import io.github.smiley4.ktoropenapi.delete
import io.github.smiley4.ktoropenapi.get
import io.github.smiley4.ktoropenapi.post
import io.github.smiley4.ktoropenapi.put
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.routing.routing

fun Application.configureUsers() {
    routing {
        authenticate(Realm.ADMIN) {
            get(
                path = "/admin/user-profiles",
                builder = {
                    description = "Get all user profiles (Admin only)"
                    tags("user-profile", "admin")
                    response {
                        HttpStatusCode.OK to {
                            body<PageResult<UserProfile>>()
                        }
                    }
                }
            ) {
                call.respondKIO(
                    UserService.getProfiles(
                        Page.from<UserSearchParameters>(call.request.queryParameters)
                    )
                )
            }

            get(
                path = "/admin/user-profiles/{userId}",
                builder = {
                    description = "Get a user's profile by user ID (Admin only)"
                    tags("user-profile", "admin")
                    request {
                        pathParameter<Long>("userId") {
                            description = "ID of the user"
                        }
                    }
                    response {
                        HttpStatusCode.OK to {
                            body<UserProfile>()
                        }
                        HttpStatusCode.NotFound to {
                            body<String>()
                        }
                    }
                }
            ) {
                val userId = call.parameters["userId"]!!.toLong()
                call.respondKIO(UserService.getProfile(userId))
            }

            get(
                path = "/admin/user-profiles/{userId}/recent-activity",
                builder = {
                    description = "Get a user's recent activity by user ID (Admin only)"
                    tags("user-profile", "admin")
                    request {
                        pathParameter<Long>("userId") {
                            description = "ID of the user"
                        }
                    }
                    response {
                        HttpStatusCode.OK to {
                            body<PageResult<String>>()
                        }
                        HttpStatusCode.NotFound to {
                            body<String>()
                        }
                    }
                }
            ) {
                val userId = call.parameters["userId"]!!.toLong()
                call.respondKIO(UserService.getRecentActivity(userId))
            }

            post(
                path = "/admin/user-profiles/block",
                builder = {
                    description = "Add a user to the login blacklist (Admin only)"
                    tags("user-profile", "admin")
                    request {
                        body<BlacklistUserRequest>()
                    }
                    response {
                        HttpStatusCode.OK to {
                            body<Unit>()
                        }
                        HttpStatusCode.NotFound to {
                            body<String>()
                        }
                    }
                }
            ) {
                val request = call.receive<BlacklistUserRequest>()
                call.respondKIO(UserService.addUserToBlacklist(request))
            }

            put(
                path = "/admin/user-profiles",
                builder = {
                    description = "Update a user's profile by user ID (Admin only)"
                    tags("user-profile", "admin")
                    request {
                        body<UpdateUserRequest>()
                    }
                    response {
                        HttpStatusCode.OK to {
                            body<Unit>()
                        }
                        HttpStatusCode.NotFound to {
                            body<String>()
                        }
                    }
                }
            ) {
                val request = call.receive<UpdateUserRequest>()
                call.respondKIO(UserService.updateProfile(request))
            }
        }

        authenticate(Realm.COMMON) {
            get(
                path = "/user-profile",
                builder = {
                    description = "Get the authenticated user's profile"
                    tags("user-profile")
                    response {
                        HttpStatusCode.OK to {
                            body<UserProfile>()
                        }
                        HttpStatusCode.NotFound to {
                            body<String>()
                        }
                    }
                }
            ) {
                val user = call.principal<LoggedInUser>()!!
                call.respondKIO(UserService.getProfile(user.id))
            }

            post(
                path = "/user-profile",
                builder = {
                    description = "Create a user profile"
                    tags("user-profile")
                    request {
                        body<CreateUserProfileRequest>()
                    }
                    response {
                        HttpStatusCode.Created to {
                            body<UserProfile>()
                        }
                        HttpStatusCode.BadRequest to {
                            body<String>()
                        }
                    }
                }
            ) {
                val user = call.principal<LoggedInUser>()!!
                val request = call.receive<CreateUserProfileRequest>()
                call.respondKIO(UserService.createProfile(user.id, request))
            }

            put(
                path = "/user-profile",
                builder = {
                    description = "Update the authenticated user's profile"
                    tags("user-profile")
                    request {
                        body<UpdateUserProfileRequest>()
                    }
                    response {
                        HttpStatusCode.OK to {
                            body<UserProfile>()
                        }
                        HttpStatusCode.NotFound to {
                            body<String>()
                        }
                    }
                }
            ) {
                val user = call.principal<LoggedInUser>()!!
                val request = call.receive<UpdateUserProfileRequest>()
                call.respondKIO(UserService.updateProfile(user.id, request))
            }

            delete(
                path = "/user-profile",
                builder = {
                    description = "Delete the authenticated user's profile"
                    tags("user-profile")
                    response {
                        HttpStatusCode.NoContent to {}
                        HttpStatusCode.BadRequest to {
                            body<String>()
                        }
                    }
                }
            ) {
                val user = call.principal<LoggedInUser>()!!
                call.respondKIO(UserService.deleteProfile(user))
            }
        }
    }
}