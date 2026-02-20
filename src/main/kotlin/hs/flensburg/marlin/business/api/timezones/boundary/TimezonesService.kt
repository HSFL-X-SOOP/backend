package hs.flensburg.marlin.business.api.timezones.boundary

import de.lambda9.tailwind.core.KIO
import hs.flensburg.marlin.Config
import hs.flensburg.marlin.business.App
import hs.flensburg.marlin.business.ServiceLayerError
import hs.flensburg.marlin.business.api.auth.boundary.IPAddressLookupService
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.time.OffsetDateTime
import kotlin.time.ExperimentalTime
import kotlin.time.toKotlinInstant

object TimezonesService {
    private val logger = KotlinLogging.logger { }
    private lateinit var ipConfig: Config.IPInfo

    fun init(config: Config.IPInfo) {
        ipConfig = config
    }

    @OptIn(ExperimentalTime::class)
    fun toLocalDateTimeInZone(utcTime: OffsetDateTime, timezone: String?): LocalDateTime {
        // Convert OffsetDateTime to Instant, then to Kotlin Instant
        val instant = utcTime.toInstant().toKotlinInstant()

        // Determine the TimeZone, defaulting to UTC if invalid or null
        val zone: TimeZone = if (!timezone.isNullOrBlank() && isValidTimezone(timezone)) {
            TimeZone.of(timezone)
        } else {
            TimeZone.UTC
        }
        // Convert the instant to LocalDateTime in the specified timezone
        return instant.toLocalDateTime(zone)
    }

    fun toLocalDateInZone(utcTime: OffsetDateTime, timezone: String?): LocalDate {
        return toLocalDateTimeInZone(utcTime, timezone).date
    }

    fun getClientTimeZoneFromIPOrQueryParam(timezone: String?, clientIp: String): String {
        // optional query param overwrites IP-based timezone
        if (timezone != null && isValidTimezone(timezone)) {
            return timezone
        }

        //val clientIp = "178.238.11.6" //uk // "85.214.132.117" //german //testing
        try {
            val ipInfo = IPAddressLookupService.lookUpIpAddressInfo(clientIp, ipConfig)
            if (ipInfo.timezone != null && isValidTimezone(ipInfo.timezone)) {
                return ipInfo.timezone
            }
        } catch (e: Exception) {
            logger.error(e) { "Failed to lookup timezone for IP $clientIp: ${e.message}" }
        }

        return "UTC"
    }


    // This function is used in routing to determine the timezone
    // is provided or retrieved via Ipaddress
    fun withResolvedTimezone(
        timezoneParam: String?,
        remoteIp: String
    ): String {
        val timezone = getClientTimeZoneFromIPOrQueryParam(
            timezone = timezoneParam ?: "DEFAULT",
            clientIp = remoteIp
        )

        return timezone
    }


    private fun isValidTimezone(tz: String): Boolean =
        try {
            TimeZone.of(tz)
            true
        } catch (e: Exception) {
            false
        }
}