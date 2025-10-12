package hs.flensburg.marlin.business.api.timezones.boundary

import de.lambda9.tailwind.core.KIO
import hs.flensburg.marlin.business.App
import hs.flensburg.marlin.business.JEnv
import hs.flensburg.marlin.business.api.auth.boundary.IPAddressLookupService
import io.ktor.server.application.ApplicationCall
import io.ktor.server.plugins.origin
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
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


    private fun isValidTimezone(tz: String): Boolean =
        try {
            TimeZone.of(tz)
            true
        } catch (e: Exception) {
            false
        }
}