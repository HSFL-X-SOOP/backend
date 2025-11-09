package hs.flensburg.marlin.business.api.location.boundary

import hs.flensburg.marlin.business.api.location.entity.Contact

object StringValidationService {
    private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    private val PHONE_REGEX = Regex("^[+]?[0-9\\s\\-()]{7,20}$")
    private val WEBSITE_REGEX =
        Regex("^(https?://)?([\\da-z.-]+)\\.([a-z.]{2,6})([/\\w .-]*)*/?$", RegexOption.IGNORE_CASE)

    // Opening hours format examples:
    // "08:00-18:00"
    // "08:00-10:00,11:00-18:00"
    // "Mon-Fri: 08:00-18:00"
    // "Mon-Fri: 08:00-10:00,11:00-18:00; Sat: 09:00-14:00"
    private val TIME_RANGE_REGEX = Regex("^\\d{2}:\\d{2}-\\d{2}:\\d{2}$")
    private val DAY_REGEX = Regex("[a-zA-Z\\-]")

    fun isValidEmail(email: String?): Boolean {
        if (email.isNullOrBlank()) return true
        return EMAIL_REGEX.matches(email.trim())
    }

    fun isValidPhone(phone: String?): Boolean {
        if (phone.isNullOrBlank()) return true
        return PHONE_REGEX.matches(phone.trim())
    }

    fun isValidWebsite(website: String?): Boolean {
        if (website.isNullOrBlank()) return true
        return WEBSITE_REGEX.matches(website.trim())
    }

    fun isValidOpeningHours(openingHours: String?): Boolean {
        if (openingHours.isNullOrBlank()) return true

        // Split by semicolon for different days
        val dayGroups = openingHours.split(";").map { it.trim() }

        return dayGroups.all { dayGroup ->
            // Extract time ranges after day prefix
            val timeRanges =
                if (dayGroup.contains(":")) {
                    val partBeforeColon = dayGroup.substringBefore(":")

                    val isHourPrefix = partBeforeColon.length in 1..2 && partBeforeColon.all { it.isDigit() }
                    val isDayPrefix = partBeforeColon.contains(DAY_REGEX)

                    if (isDayPrefix && !isHourPrefix) {
                        // Remove day prefix and trim
                        dayGroup.substringAfter(":").trim()
                    } else {
                        dayGroup.trim()
                    }
                } else {
                    dayGroup
                }

            // Split by comma for multiple time ranges in a day
            val ranges = timeRanges.split(",").map { it.trim() }

            // Validate each time range
            ranges.all { range ->
                if (!TIME_RANGE_REGEX.matches(range)) return@all false

                // Validate time format and logical order
                val (start, end) = range.split("-")
                val startValid = isValidTime(start)
                val endValid = isValidTime(end)

                startValid && endValid && isStartBeforeEnd(start, end)
            }
        }
    }

    fun validateContact(contact: Contact?): String? {
        if (contact == null) return null

        if (!isValidEmail(contact.email)) {
            return "Invalid email format"
        }

        if (!isValidPhone(contact.phone)) {
            return "Invalid phone format. Expected format: +49 123 456789 or (123) 456-7890"
        }

        if (!isValidWebsite(contact.website)) {
            return "Invalid website format. Expected format: https://example.com"
        }

        return null
    }

    fun validateOpeningHoursFormat(openingHours: String?): String? {
        if (openingHours.isNullOrBlank()) return null

        if (!isValidOpeningHours(openingHours)) {
            return "Invalid opening hours format. Expected format: '08:00-18:00' or '08:00-10:00,11:00-18:00' or 'Mon-Fri: 08:00-18:00' or 'Mon-Fri: 08:00-10:00,11:00-18:00; Sat: 09:00-14:00"
        }

        return null
    }

    private fun isValidTime(time: String): Boolean {
        val parts = time.split(":")
        if (parts.size != 2) return false

        val hour = parts[0].toIntOrNull() ?: return false
        val minute = parts[1].toIntOrNull() ?: return false

        return hour in 0..23 && minute in 0..59
    }

    private fun isStartBeforeEnd(start: String, end: String): Boolean {
        val (startHour, startMin) = start.split(":").map { it.toInt() }
        val (endHour, endMin) = end.split(":").map { it.toInt() }

        val startMinutes = startHour * 60 + startMin
        val endMinutes = endHour * 60 + endMin

        return startMinutes < endMinutes
    }
}