package hs.flensburg.marlin.business.api.users.boundary

import de.lambda9.tailwind.core.KIO
import de.lambda9.tailwind.core.extensions.kio.onNullFail
import de.lambda9.tailwind.core.extensions.kio.orDie
import hs.flensburg.marlin.business.ApiError
import hs.flensburg.marlin.business.App
import hs.flensburg.marlin.business.ServiceLayerError
import hs.flensburg.marlin.business.api.auth.boundary.AuthService
import hs.flensburg.marlin.business.api.auth.entity.LoggedInUser
import hs.flensburg.marlin.business.api.users.control.UserRepo
import hs.flensburg.marlin.business.api.users.entity.CreateUserProfileRequest
import hs.flensburg.marlin.business.api.users.entity.UpdateUserProfileRequest
import hs.flensburg.marlin.business.api.users.entity.UserProfileResponse

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

    fun getProfile(userId: Long): App<Error, UserProfileResponse> = KIO.comprehension {
        val profile = !UserRepo.fetchProfileByUserId(userId).orDie().onNullFail { Error.NotFound }
        KIO.ok(UserProfileResponse.from(profile))
    }

    fun createProfile(
        userId: Long,
        profile: CreateUserProfileRequest
    ): App<Error, UserProfileResponse> = KIO.comprehension {
        val created = !UserRepo.insertProfile(userId, profile.roles, profile.language, profile.measurementSystem).orDie()
        KIO.ok(UserProfileResponse.from(created))
    }

    fun updateProfile(userId: Long, request: UpdateUserProfileRequest): App<Error, UserProfileResponse> = KIO.comprehension {
        val profile = !UserRepo.updateProfile(
            userId = userId,
            language = request.language,
            roles = request.roles,
            measurementSystem = request.measurementSystem
        ).orDie().onNullFail { Error.NotFound }

        KIO.ok(UserProfileResponse.from(profile))
    }

    fun deleteProfile(loggedInUser: LoggedInUser): App<AuthService.Error, Unit> = KIO.comprehension {
        val user = !UserRepo.fetchById(loggedInUser.id).orDie().onNullFail { AuthService.Error.BadRequest }

        UserRepo.deleteById(user.id!!).orDie()
    }
}