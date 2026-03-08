package hs.flensburg.marlin.business.schedulerJobs.anomalyDetection.control

import de.lambda9.tailwind.jooq.Jooq
import hs.flensburg.marlin.business.api.sensors.entity.EnrichedMeasurementDTO
import hs.flensburg.marlin.database.generated.tables.references.ANOMALIES
import kotlinx.datetime.toJavaLocalDateTime
import java.time.ZoneOffset

object AnomalyDetectionRepo {
    fun writeAnomaly(measurement: EnrichedMeasurementDTO) = Jooq.query {

        val time = measurement.time!!.toJavaLocalDateTime().atOffset(ZoneOffset.UTC)

        val anomalyId = insertInto(ANOMALIES)
            .columns(
                ANOMALIES.MEASUREMENT_SENSOR_ID,
                ANOMALIES.MEASUREMENT_TYPE_ID,
                ANOMALIES.MEASUREMENT_TIME
            ).values(
                measurement.sensor.id!!,
                measurement.measurementType.id,
                time
            )
            .onDuplicateKeyIgnore()
            .execute()
    }
}