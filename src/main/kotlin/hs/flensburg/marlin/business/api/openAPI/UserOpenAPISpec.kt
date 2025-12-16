package hs.flensburg.marlin.business.api.openAPI

import hs.flensburg.marlin.business.PageResult
import hs.flensburg.marlin.business.api.users.entity.BlacklistUserRequest
import hs.flensburg.marlin.business.api.users.entity.CreateUserProfileRequest
import hs.flensburg.marlin.business.api.users.entity.UpdateUserProfileRequest
import hs.flensburg.marlin.business.api.users.entity.UpdateUserRequest
import hs.flensburg.marlin.business.api.users.entity.UserProfile
import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.ktor.http.HttpStatusCode


object UserOpenAPISpec {

    val getAllUserProfiles: RouteConfig.() -> Unit = {
        tags("admin")
        description = "Get all user profiles with optional filtering and pagination. " +
                "Returns a paginated list of user profiles. Supports extensive filtering by user attributes, " +
                "timestamps, and sorting. Admin access required."

        request {
            queryParameter<Long>("id") {
                description = "Filter by user ID"
                required = false
            }
            queryParameter<String>("email") {
                description = "Filter by email (case-insensitive like match)"
                required = false
            }
            queryParameter<Boolean>("verified") {
                description = "Filter by verification status"
                required = false
            }
            queryParameter<String>("authorityRole") {
                description = "Filter by authority role (USER, ADMIN, etc.)"
                required = false
            }
            queryParameter<List<String>>("activityRoles") {
                description = "Filter by activity roles (can provide multiple)"
                required = false
            }
            queryParameter<String>("language") {
                description = "Filter by preferred language"
                required = false
            }
            queryParameter<String>("measurementSystem") {
                description = "Filter by measurement system"
                required = false
            }
            queryParameter<String>("userCreatedAt") {
                description = "Filter by user creation timestamp (ISO-8601 format)"
                required = false
            }
            queryParameter<String>("userUpdatedAt") {
                description = "Filter by user update timestamp (ISO-8601 format)"
                required = false
            }
            queryParameter<String>("profileCreatedAt") {
                description = "Filter by profile creation timestamp (ISO-8601 format)"
                required = false
            }
            queryParameter<String>("profileUpdatedAt") {
                description = "Filter by profile update timestamp (ISO-8601 format)"
                required = false
            }
            queryParameter<String>("sort") {
                description = "Sort field in snake_case. Use 'field.asc' or 'field.desc' (e.g., 'id.asc')."
                required = false
            }
            queryParameter<Long>("limit") {
                description = "Maximum number of results to return"
                required = false
            }
            queryParameter<Long>("offset") {
                description = "The offset from the start of the results"
                required = false
            }
        }

        response {
            HttpStatusCode.OK to {
                description = "User profiles retrieved successfully"
                body<PageResult<UserProfile>>()
            }
        }
    }

    val getUserProfileById: RouteConfig.() -> Unit = {
        tags("admin")
        description = "Get a specific user's profile by user ID. " +
                "Returns the complete user profile information. Admin access required."

        request {
            pathParameter<Long>("userId") {
                description = "ID of the user"
            }
        }

        response {
            HttpStatusCode.OK to {
                description = "User profile retrieved successfully"
                body<UserProfile>()
            }
            HttpStatusCode.NotFound to {
                description = "User not found"
                body<String>()
            }
        }
    }

    val getUserRecentActivity: RouteConfig.() -> Unit = {
        tags("admin")
        description = "Get a user's recent activity by user ID. " +
                "Returns a paginated list of recent user activities. Admin access required."

        request {
            pathParameter<Long>("userId") {
                description = "ID of the user"
            }
        }

        response {
            HttpStatusCode.OK to {
                description = "User activity retrieved successfully"
                body<PageResult<String>>()
            }
            HttpStatusCode.NotFound to {
                description = "User not found"
                body<String>()
            }
        }
    }

    val blockUser: RouteConfig.() -> Unit = {
        tags("admin")
        description = "Add a user to the login blacklist, preventing them from authenticating. " +
                "Requires a reason for blacklisting. Admin access required."

        request {
            body<BlacklistUserRequest>()
        }

        response {
            HttpStatusCode.OK to {
                description = "User successfully added to blacklist"
                body<Unit>()
            }
            HttpStatusCode.NotFound to {
                description = "User not found"
                body<String>()
            }
        }
    }

    val updateUserProfileAdmin: RouteConfig.() -> Unit = {
        tags("admin")
        description = "Update a user's profile by user ID. " +
                "Allows administrators to modify user profile information. Admin access required."

        request {
            body<UpdateUserRequest>()
        }

        response {
            HttpStatusCode.OK to {
                description = "User profile updated successfully"
                body<Unit>()
            }
            HttpStatusCode.NotFound to {
                description = "User not found"
                body<String>()
            }
        }
    }

    val deleteUserAdmin: RouteConfig.() -> Unit = {
        tags("admin")
        description = "Delete a user's profile and account by user ID. " +
                "Permanently removes the user from the system. Admin access required."

        request {
            pathParameter<Long>("userId") {
                description = "ID of the user"
            }
        }

        response {
            HttpStatusCode.NoContent to {
                description = "User successfully deleted"
            }
            HttpStatusCode.NotFound to {
                description = "User not found"
                body<String>()
            }
        }
    }

    val getCurrentUserProfile: RouteConfig.() -> Unit = {
        tags("user-profile")
        description = "Get the authenticated user's profile. " +
                "Returns the profile information for the currently logged-in user. " +
                "Requires a valid JWT access token in the Authorization header."

        securitySchemeNames("BearerAuth", "BearerAuthAdmin")

        response {
            HttpStatusCode.OK to {
                description = "User profile retrieved successfully"
                body<UserProfile>()
            }
            HttpStatusCode.NotFound to {
                description = "User profile not found"
                body<String>()
            }
            HttpStatusCode.Unauthorized to {
                description = "Missing or invalid JWT token"
                body<String>()
            }
        }
    }

    val createUserProfile: RouteConfig.() -> Unit = {
        tags("user-profile")
        description = "Create a user profile for the authenticated user. " +
                "Initializes profile settings such as language, measurement system, and other preferences. " +
                "Requires a valid JWT access token in the Authorization header."

        securitySchemeNames("BearerAuth", "BearerAuthAdmin")

        request {
            body<CreateUserProfileRequest>()
        }

        response {
            HttpStatusCode.Created to {
                description = "User profile created successfully"
                body<UserProfile>()
            }
            HttpStatusCode.BadRequest to {
                description = "Invalid profile data"
                body<String>()
            }
            HttpStatusCode.Unauthorized to {
                description = "Missing or invalid JWT token"
                body<String>()
            }
        }
    }

    val updateCurrentUserProfile: RouteConfig.() -> Unit = {
        tags("user-profile")
        description = "Update the authenticated user's profile. " +
                "Allows users to modify their own profile settings and preferences. " +
                "Requires a valid JWT access token in the Authorization header."

        securitySchemeNames("BearerAuth", "BearerAuthAdmin")

        request {
            body<UpdateUserProfileRequest>()
        }

        response {
            HttpStatusCode.OK to {
                description = "User profile updated successfully"
                body<UserProfile>()
            }
            HttpStatusCode.NotFound to {
                description = "User profile not found"
                body<String>()
            }
            HttpStatusCode.Unauthorized to {
                description = "Missing or invalid JWT token"
                body<String>()
            }
        }
    }

    val deleteCurrentUser: RouteConfig.() -> Unit = {
        tags("user-profile")
        description = "Delete the authenticated user from the system. " +
                "Permanently removes the user's account and all associated data. " +
                "Requires a valid JWT access token in the Authorization header."

        securitySchemeNames("BearerAuth", "BearerAuthAdmin")

        response {
            HttpStatusCode.NoContent to {
                description = "User successfully deleted"
            }
            HttpStatusCode.BadRequest to {
                description = "Invalid deletion request"
                body<String>()
            }
            HttpStatusCode.Unauthorized to {
                description = "Missing or invalid JWT token"
                body<String>()
            }
        }
    }
}
