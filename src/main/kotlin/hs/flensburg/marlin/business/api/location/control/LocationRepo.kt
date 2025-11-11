package hs.flensburg.marlin.business.api.location.control

import de.lambda9.tailwind.jooq.JIO
import de.lambda9.tailwind.jooq.Jooq
import hs.flensburg.marlin.database.generated.tables.pojos.Location
import hs.flensburg.marlin.database.generated.tables.pojos.LocationImage
import hs.flensburg.marlin.database.generated.tables.references.LOCATION
import hs.flensburg.marlin.database.generated.tables.references.LOCATION_IMAGE
import java.time.LocalDateTime

object LocationRepo {

    fun fetchLocationByID(id: Long): JIO<Location?> = Jooq.query {
        selectFrom(LOCATION)
            .where(LOCATION.ID.eq(id))
            .fetchOneInto(Location::class.java)
    }

    fun fetchAllLocations(): JIO<List<Location>> = Jooq.query {
        selectFrom(LOCATION)
            .orderBy(LOCATION.ID.asc())
            .fetchInto(Location::class.java)
    }

    fun fetchLocationsWithoutNameOrAdressButCoordinates(): JIO<List<Location>> = Jooq.query {
        selectFrom(LOCATION)
            .where(LOCATION.NAME.isNull)
            .or(LOCATION.ADDRESS.isNull)
            .and(LOCATION.COORDINATES.isNotNull)
            .fetchInto(Location::class.java)
    }

    fun countAllLocations(): JIO<Int> = Jooq.query {
        selectCount()
            .from(LOCATION)
            .fetchOneInto(Int::class.java) ?: 0
    }

    fun updateLocation(
        id: Long,
        name: String?,
        description: String?,
        address: String?,
        openingHours: String?,
        phone: String?,
        email: String?,
        website: String?
    ): JIO<Location?> = Jooq.query {
        update(LOCATION)
            .set(LOCATION.NAME, name)
            .set(LOCATION.DESCRIPTION, description)
            .set(LOCATION.ADDRESS, address)
            .set(LOCATION.OPENING_HOURS, openingHours)
            .set(LOCATION.CONTACT_PHONE, phone)
            .set(LOCATION.CONTACT_EMAIL, email)
            .set(LOCATION.CONTACT_WEBSITE, website)
            .where(LOCATION.ID.eq(id))
            .returning()
            .fetchOneInto(Location::class.java)
    }

    fun insertLocationImage(id: Long, imageBytes: ByteArray, contentType: String): JIO<Unit> = Jooq.query {
        insertInto(LOCATION_IMAGE)
            .set(LOCATION_IMAGE.LOCATION_ID, id)
            .set(LOCATION_IMAGE.DATA, imageBytes)
            .set(LOCATION_IMAGE.CONTENT_TYPE, contentType)
            .execute()
    }

    fun updateLocationImage(id: Long, imageBytes: ByteArray, contentType: String): JIO<Unit> = Jooq.query {
        update(LOCATION_IMAGE)
            .set(LOCATION_IMAGE.DATA, imageBytes)
            .set(LOCATION_IMAGE.CONTENT_TYPE, contentType)
            .set(LOCATION_IMAGE.UPLOADED_AT, LocalDateTime.now())
            .where(LOCATION_IMAGE.LOCATION_ID.eq(id))
            .execute()
    }

    fun fetchLocationImage(id: Long): JIO<LocationImage?> = Jooq.query {
        select(LOCATION_IMAGE)
            .from(LOCATION_IMAGE)
            .where(LOCATION_IMAGE.LOCATION_ID.eq(id))
            .fetchOneInto(LocationImage::class.java)
    }

    fun deleteLocationImage(id: Long): JIO<Unit> = Jooq.query {
        deleteFrom(LOCATION_IMAGE)
            .where(LOCATION_IMAGE.LOCATION_ID.eq(id))
            .execute()
    }

}