package hs.flensburg.marlin.business.api.auth.control

import de.lambda9.tailwind.core.KIO
import de.lambda9.tailwind.core.extensions.kio.orDie
import hs.flensburg.marlin.business.ApiError
import hs.flensburg.marlin.business.App
import hs.flensburg.marlin.business.ServiceLayerError
import hs.flensburg.marlin.business.api.auth.entity.IPAddressLookupResponse
import hs.flensburg.marlin.business.api.email.boundary.EmailService
import hs.flensburg.marlin.business.httpclient
import hs.flensburg.marlin.database.generated.tables.records.LoginBlacklistRecord
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.time.LocalDateTime

object BlacklistHandler {
    sealed class Error(private val message: String) : ServiceLayerError {
        object Unauthorized : Error("Unauthorized access")
        object BadRequest : Error("Bad request")
        object LoginLimitExceeded : Error("Login limit exceeded, please try again later")

        override fun toApiError(): ApiError {
            return when (this) {
                is Unauthorized -> ApiError.Unauthorized(message)
                is BadRequest -> ApiError.BadRequest(message)
                is LoginLimitExceeded -> ApiError.TooManyRequests(message)
            }
        }
    }

    private const val LOGIN_BLACKLIST_DURATION_MINUTES = 15L

    fun addUserToBlacklist(
        userId: Long,
        ipAddress: String
    ): App<ServiceLayerError, Unit> = KIO.comprehension {
        val now = LocalDateTime.now()
        val ipInfo = lookUpIpAddressInfo(ipAddress)

        val record = LoginBlacklistRecord().apply {
            this.userId = userId
            this.ipAddress = ipAddress
            this.country = ipInfo.country
            this.region = ipInfo.regionName
            this.city = ipInfo.city
            this.blockedAt = now
            this.blockedUntil = now.plusMinutes(LOGIN_BLACKLIST_DURATION_MINUTES)
        }

        !AuthRepo.insertLoginBlacklist(record).orDie()

        !EmailService.sendBlacklistNotificationEmail(userId)

        KIO.unit
    }

    private fun lookUpIpAddressInfo(ipAddress: String): IPAddressLookupResponse = runBlocking {
        httpclient.get("http://ip-api.com/json/$ipAddress").body<IPAddressLookupResponse>()
    }
}