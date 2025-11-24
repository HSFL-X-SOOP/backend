import de.lambda9.tailwind.jooq.JIO
import de.lambda9.tailwind.jooq.Jooq
import hs.flensburg.marlin.business.ApiError
import hs.flensburg.marlin.business.ServiceLayerError
import hs.flensburg.marlin.database.generated.tables.pojos.NotificationLocations
import hs.flensburg.marlin.database.generated.tables.references.NOTIFICATION_LOCATIONS

object NotificationLocationsRepo {
    sealed class Error(private val message: String) : ServiceLayerError {
        object NotFound : Error("Notification Location not found")
        object BadRequest : Error("Bad request")

        override fun toApiError(): ApiError {
            return when (this) {
                is NotFound -> ApiError.NotFound(message)
                is BadRequest -> ApiError.BadRequest(message)
            }
        }
    }

    fun insert(notificationLocation: NotificationLocations): JIO<NotificationLocations> = Jooq.query {
        insertInto(NOTIFICATION_LOCATIONS)
            .set(NOTIFICATION_LOCATIONS.LOCATION_ID, notificationLocation.locationId)
            .set(NOTIFICATION_LOCATIONS.NOTIFICATION_TITLE, notificationLocation.notificationTitle)
            .set(NOTIFICATION_LOCATIONS.NOTIFICATION_TEXT, notificationLocation.notificationText)
            .set(NOTIFICATION_LOCATIONS.CREATED_BY, notificationLocation.createdBy)
            .returning()
            .fetchInto(NotificationLocations::class.java).first()
    }

    fun fetchById(id: Long): JIO<NotificationLocations?> = Jooq.query {
        selectFrom(NOTIFICATION_LOCATIONS)
            .where(NOTIFICATION_LOCATIONS.ID.eq(id))
            .fetchOneInto(NotificationLocations::class.java)
    }

    fun fetchAllByLocationId(locationId: Long): JIO<List<NotificationLocationDTO?>> = Jooq.query {
        selectFrom(NOTIFICATION_LOCATIONS)
            .where(NOTIFICATION_LOCATIONS.LOCATION_ID.eq(locationId))
            .fetchInto(NotificationLocations::class.java)
            .map { NotificationLocationDTO.from(it) }
    }

    fun deleteById(id: Long): JIO<Unit> = Jooq.query {
        deleteFrom(NOTIFICATION_LOCATIONS)
            .where(NOTIFICATION_LOCATIONS.ID.eq(id))
            .execute()
    }
}