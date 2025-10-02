package hs.flensburg.marlin.business.api.sensors.entity.raw

import kotlinx.serialization.Serializable
import hs.flensburg.marlin.database.generated.tables.pojos.Measurement
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.toKotlinInstant


@Serializable
data class MeasurementDTO(
    val sensorId: Long,
    val typeId: Long,
    val locationId: Long?,
    val time: LocalDateTime,
    val value: Double
)

@OptIn(ExperimentalTime::class)
fun Measurement.toMeasurementDTO() = MeasurementDTO(
    sensorId = this.sensorId ?: 0L,
    typeId = this.typeId ?: 0L,
    locationId = this.locationId,
    time = this.time?.toInstant()?.toKotlinInstant()?.toLocalDateTime(TimeZone.UTC) ?: Clock.System.now().toLocalDateTime(TimeZone.UTC),
    value = this.value ?: 0.0
)

