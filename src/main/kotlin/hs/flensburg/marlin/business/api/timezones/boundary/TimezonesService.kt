package hs.flensburg.marlin.business.api.timezones.boundary

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

    fun getClientTimeZoneFromIPOrQueryParam(call: ApplicationCall): String {
        // optional query param overwrites IP-based timezone
        val timezone = call.parameters["timezone"] ?: "DEFAULT"
        if (timezone != "DEFAULT" && isValidTimezone(timezone)) {
            return timezone
        }

        // IP based timezone
        val clientIp = call.request.origin.remoteAddress
        //val clientIp = "178.238.11.6" //uk // "85.214.132.117" //german //testing
        val ipInfo = IPAddressLookupService.lookUpIpAddressInfo(clientIp)
        if (ipInfo.timezone != null && isValidTimezone(ipInfo.timezone)) {
            return ipInfo.timezone
        }

        // fallback to UTC
        return "UTC"
    }


    private fun isValidTimezone(tz: String): Boolean =
        try {
            TimeZone.of(tz)
            true
        } catch (e: Exception) {
            false
        }
}