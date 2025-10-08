package hs.flensburg.marlin.business.schedulerJobs.potentialSensors.control

import de.lambda9.tailwind.jooq.JIO
import de.lambda9.tailwind.jooq.Jooq
import hs.flensburg.marlin.database.generated.tables.pojos.PotentialSensor
import hs.flensburg.marlin.database.generated.tables.references.POTENTIAL_SENSOR
import org.jooq.impl.DSL
import org.jooq.impl.DSL.max

object PotentialSensorRepo {
    fun fetchMaxPotentialSensorId(): JIO<Long?> = Jooq.query {
        select(max(POTENTIAL_SENSOR.ID))
            .from(POTENTIAL_SENSOR)
            .fetchOne(0, Long::class.java)
    }

    fun fetchActivePotentialSensorIds(): JIO<List<Long>> = Jooq.query {
        select(POTENTIAL_SENSOR.ID)
            .from(POTENTIAL_SENSOR)
            .where(POTENTIAL_SENSOR.IS_ACTIVE.eq(true))
            .fetchInto(Long::class.java)
    }

    fun insertPotentialSensor(
        id: Long,
        name: String,
        description: String?,
        isActive: Boolean = false

    ): JIO<PotentialSensor> = Jooq.query {
        insertInto(POTENTIAL_SENSOR)
            .set(POTENTIAL_SENSOR.ID, id)
            .set(POTENTIAL_SENSOR.NAME, name)
            .set(POTENTIAL_SENSOR.DESCRIPTION, description)
            .set(POTENTIAL_SENSOR.IS_ACTIVE, isActive)
            .onDuplicateKeyIgnore()
            .returning()
            .fetchOneInto(PotentialSensor:: class.java)!!
    }

    fun fetchAllPotentialSensors(): JIO<List<PotentialSensor>> = Jooq.query {
        selectFrom(POTENTIAL_SENSOR)
            .orderBy(POTENTIAL_SENSOR.ID.asc())
            .fetchInto(PotentialSensor::class.java)
    }

    fun updateIsActive(id: Long): JIO<PotentialSensor> = Jooq.query {
        update(POTENTIAL_SENSOR)
            .set(POTENTIAL_SENSOR.IS_ACTIVE, DSL.not(POTENTIAL_SENSOR.IS_ACTIVE))
            .where(POTENTIAL_SENSOR.ID.eq(id))
            .returning()
            .fetchOneInto(PotentialSensor::class.java)!!
    }

}