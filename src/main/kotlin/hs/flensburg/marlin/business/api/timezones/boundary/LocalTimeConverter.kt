package hs.flensburg.marlin.business.api.timezones.boundary

fun kotlinx.datetime.LocalTime.toJavaLocalTime(): java.time.LocalTime = java.time.LocalTime.of(hour, minute)

fun java.time.LocalTime.toKotlinxLocalTime() = kotlinx.datetime.LocalTime(hour, minute)