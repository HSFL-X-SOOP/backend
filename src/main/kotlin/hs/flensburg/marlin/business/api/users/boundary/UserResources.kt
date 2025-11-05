package hs.flensburg.marlin.business.api.users.boundary

import hs.flensburg.marlin.business.api.auth.entity.LoggedInUser
import hs.flensburg.marlin.business.api.users.entity.CreateUserProfileRequest
import hs.flensburg.marlin.business.api.users.entity.UpdateUserProfileRequest
import hs.flensburg.marlin.business.api.users.entity.UserProfileResponse
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
        authenticate(Realm.COMMON) {
            get(
                path = "/user-profile",
                builder = {
                    description = "Get the authenticated user's profile"
                    tags("user-profile")
                    securitySchemeNames("BearerAuth", "BearerAuthAdmin")
                    response {
                        HttpStatusCode.OK to {
                            body<UserProfileResponse>()
                        }
                        HttpStatusCode.NotFound to {
                            body<String>()
                        }
                        HttpStatusCode.Unauthorized to {
                            description = "Missing or invalid JWT token"
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
                    securitySchemeNames("BearerAuth", "BearerAuthAdmin")
                    request {
                        body<CreateUserProfileRequest>()
                    }
                    response {
                        HttpStatusCode.Created to {
                            body<UserProfileResponse>()
                        }
                        HttpStatusCode.BadRequest to {
                            body<String>()
                        }
                        HttpStatusCode.Unauthorized to {
                            description = "Missing or invalid JWT token"
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
                    securitySchemeNames("BearerAuth", "BearerAuthAdmin")
                    request {
                        body<UpdateUserProfileRequest>()
                    }
                    response {
                        HttpStatusCode.OK to {
                            body<UserProfileResponse>()
                        }
                        HttpStatusCode.NotFound to {
                            body<String>()
                        }
                        HttpStatusCode.Unauthorized to {
                            description = "Missing or invalid JWT token"
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
                    securitySchemeNames("BearerAuth", "BearerAuthAdmin")
                    response {
                        HttpStatusCode.NoContent to {}
                        HttpStatusCode.BadRequest to {
                            body<String>()
                        }
                        HttpStatusCode.Unauthorized to {
                            description = "Missing or invalid JWT token"
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