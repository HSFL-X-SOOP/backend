package hs.flensburg.marlin.business.api.location.control

import de.lambda9.tailwind.jooq.JIO
import de.lambda9.tailwind.jooq.Jooq
import hs.flensburg.marlin.database.generated.tables.pojos.Location
import hs.flensburg.marlin.database.generated.tables.references.LOCATION
import java.time.LocalTime

object LocationRepo {

    fun fetchLocationByID(id: Long): JIO<Location?> = Jooq.query {
        selectFrom(LOCATION)
            .where(LOCATION.ID.eq(id))
            .fetchOneInto(Location::class.java)
    }

    fun updateLocation(
        id: Long,
        name: String?,
        description: String?,
        address: String?,
        openingTime: LocalTime?,
        closingTime: LocalTime?,
    ): JIO<Location?> = Jooq.query {
        update(LOCATION)
            .set(LOCATION.NAME, name)
            .set(LOCATION.DESCRIPTION, description)
            .set(LOCATION.ADDRESS, address)
            .set(LOCATION.OPENING_TIME, openingTime)
            .set(LOCATION.CLOSING_TIME, closingTime)
            .where(LOCATION.ID.eq(id))
            .returning()
            .fetchOneInto(Location::class.java)
    }
}