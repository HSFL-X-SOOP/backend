package hs.flensburg.marlin.business.api.location.control

import de.lambda9.tailwind.jooq.JIO
import de.lambda9.tailwind.jooq.Jooq
import hs.flensburg.marlin.business.api.sensors.entity.raw.GeoPointDTO
import hs.flensburg.marlin.business.api.sensors.entity.raw.LocationDTO
import hs.flensburg.marlin.database.generated.tables.pojos.Location
import hs.flensburg.marlin.database.generated.tables.references.LOCATION
import org.jooq.SelectField
import org.jooq.impl.DSL

object LocationRepo {

    fun fetchLocationByID(id: Long): JIO<Location?> = Jooq.query {
        selectFrom(LOCATION)
            .where(LOCATION.ID.eq(id))
            .fetchOneInto(Location::class.java)
    }
}