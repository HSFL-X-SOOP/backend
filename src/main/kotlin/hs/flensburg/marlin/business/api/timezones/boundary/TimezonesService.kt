package hs.flensburg.marlin.business.api.timezones.boundary

import de.lambda9.tailwind.core.KIO
import hs.flensburg.marlin.business.App
import hs.flensburg.marlin.business.JEnv
import hs.flensburg.marlin.business.ServiceLayerError
import hs.flensburg.marlin.business.api.auth.boundary.IPAddressLookupService
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.time.OffsetDateTime
import kotlin.time.ExperimentalTime
import kotlin.time.toKotlinInstant

object TimezonesService {

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

    fun getClientTimeZoneFromIPOrQueryParam(timezone: String, clientIp: String): App<Nothing, String> =
        KIO.comprehension {
            // optional query param overwrites IP-based timezone
            if (timezone != "DEFAULT" && isValidTimezone(timezone)) {
                // Early return
                return@comprehension KIO.ok(timezone)
            }

            // IP based timezone
            //val clientIp = "178.238.11.6" //uk // "85.214.132.117" //german //testing
            val (_, env) = !KIO.access<JEnv>()
            val ipInfo = IPAddressLookupService.lookUpIpAddressInfo(clientIp, env.config.ipInfo)
            if (ipInfo.timezone != null && isValidTimezone(ipInfo.timezone)) {
                // Early return
                return@comprehension KIO.ok(ipInfo.timezone)
            }

            // fallback to UTC
            KIO.ok("UTC")
        }

    // This function is used in routing to determine the timezone
    // is provided or retrieved via Ipaddress
    fun <T> withResolvedTimezone(
        timezoneParam: String?,
        remoteIp: String,
        block: (resolvedTimezone: String) -> App<ServiceLayerError, T>
    ): App<ServiceLayerError, T> = KIO.comprehension {

        val timezone = !getClientTimeZoneFromIPOrQueryParam(
            timezone = timezoneParam ?: "DEFAULT",
            clientIp = remoteIp
        )
        // function to run with timezone
        block(timezone)
    }


    private fun isValidTimezone(tz: String): Boolean =
        try {
            TimeZone.of(tz)
            true
        } catch (e: Exception) {
            false
        }
}