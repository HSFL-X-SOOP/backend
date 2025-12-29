package hs.flensburg.marlin.business.api.users.boundary

import de.lambda9.tailwind.core.KIO
import de.lambda9.tailwind.core.extensions.kio.onNullFail
import de.lambda9.tailwind.core.extensions.kio.orDie
import de.lambda9.tailwind.jooq.transact
import hs.flensburg.marlin.business.ApiError
import hs.flensburg.marlin.business.App
import hs.flensburg.marlin.business.Page
import hs.flensburg.marlin.business.PageResult
import hs.flensburg.marlin.business.ServiceLayerError
import hs.flensburg.marlin.business.api.auth.boundary.BlacklistHandler
import hs.flensburg.marlin.business.api.auth.entity.LoggedInUser
import hs.flensburg.marlin.business.api.location.control.LocationRepo
import hs.flensburg.marlin.business.api.sensors.entity.raw.LocationDTO
import hs.flensburg.marlin.business.api.users.control.UserRepo
import hs.flensburg.marlin.business.api.users.entity.BlacklistUserRequest
import hs.flensburg.marlin.business.api.users.entity.CreateUserProfileRequest
import hs.flensburg.marlin.business.api.users.entity.UpdateUserProfileRequest
import hs.flensburg.marlin.business.api.users.entity.UpdateUserRequest
import hs.flensburg.marlin.business.api.users.entity.UserProfile
import hs.flensburg.marlin.business.api.users.entity.UserSearchParameters
import hs.flensburg.marlin.database.generated.enums.UserAuthorityRole
import hs.flensburg.marlin.database.generated.tables.records.UserProfileRecord

object UserService {
    sealed class Error(private val message: String) : ServiceLayerError {
        object NotFound : Error("User profile not found")
        object BadRequest : Error("Bad request")

        override fun toApiError(): ApiError {
            return when (this) {
                is NotFound -> ApiError.NotFound(message)
                is BadRequest -> ApiError.BadRequest(message)
            }
        }
    }

    fun getProfiles(page: Page<UserSearchParameters>): App<Error, PageResult<UserProfile>> = KIO.comprehension {
        UserRepo.fetch(page).orDie()
    }

    fun getProfile(userId: Long): App<Error, UserProfile> = KIO.comprehension {
        val userView = !UserRepo.fetchViewById(userId).orDie().onNullFail { Error.NotFound }
        val location = userView.assignedLocationId?.let { !LocationRepo.fetchLocationByID(it).orDie() }

        KIO.ok(UserProfile.from(userView, location))
    }

    fun getRecentActivity(userId: Long): App<Error, PageResult<String>> = KIO.comprehension {
        UserRepo.fetchRecentActivity(userId).orDie()
    }

    fun createProfile(
        userId: Long,
        profile: CreateUserProfileRequest
    ): App<Error, UserProfile> = KIO.comprehension {
        !UserRepo.insertProfile(
            UserProfileRecord().apply {
                this.userId = userId
                this.language = profile.language
                this.role = profile.roles.toTypedArray()
                this.measurementSystem = profile.measurementSystem
            },
            profile.firstName,
            profile.lastName
        ).transact().orDie()
        UserRepo.fetchViewById(userId).orDie().onNullFail { Error.NotFound }.map { UserProfile.from(it) }
    }

    fun updateProfile(
        userId: Long,
        request: UpdateUserProfileRequest
    ): App<Error, UserProfile> = KIO.comprehension {
        !UserRepo.updateProfile(
            userId = userId,
            firstName = request.firstName,
            lastName = request.lastName,
            language = request.language,
            roles = request.roles,
            measurementSystem = request.measurementSystem
        ).orDie().onNullFail { Error.NotFound }

        UserRepo.fetchViewById(userId).orDie().onNullFail { Error.NotFound }.map { UserProfile.from(it) }
    }

    fun updateProfile(
        request: UpdateUserRequest
    ): App<Error, Unit> = KIO.comprehension {
        UserRepo.update(
            userId = request.userId,
            firstName = null,
            lastName = null,
            authorityRole = request.authorityRole,
            verified = request.verified,
            locationId = request.locationId
        ).orDie().onNullFail { Error.NotFound }.map { }
    }

    fun addUserToBlacklist(
        request: BlacklistUserRequest
    ): App<ServiceLayerError, Unit> = KIO.comprehension {
        BlacklistHandler.addUserToBlacklist(
            request.userId,
            ipAddress = null,
            note = request.reason,
            durationMinutes = request.blockUntil,
            sendNotificationEmail = false
        )
    }

    fun deleteUser(loggedInUser: LoggedInUser): App<Error, Unit> = KIO.comprehension {
        val user = !UserRepo.fetchById(loggedInUser.id).orDie().onNullFail { Error.BadRequest }

        UserRepo.deleteById(user.id!!).orDie()
    }

    fun deleteUser(userId: Long): App<Error, Unit> = KIO.comprehension {
        val user = !UserRepo.fetchById(userId).orDie().onNullFail { Error.BadRequest }

        UserRepo.deleteById(user.id!!).orDie()
    }
}