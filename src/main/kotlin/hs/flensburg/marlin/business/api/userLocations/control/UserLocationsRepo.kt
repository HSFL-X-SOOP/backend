import de.lambda9.tailwind.jooq.JIO
import de.lambda9.tailwind.jooq.Jooq
import hs.flensburg.marlin.business.ApiError
import hs.flensburg.marlin.business.ServiceLayerError
import hs.flensburg.marlin.database.generated.tables.pojos.UserLocations
import hs.flensburg.marlin.database.generated.tables.references.USER_LOCATIONS

object UserLocationsRepo {
    sealed class Error(private val message: String) : ServiceLayerError {
        object NotFound : Error("User Location not found")
        object BadRequest : Error("Bad request")

        override fun toApiError(): ApiError {
            return when (this) {
                is NotFound -> ApiError.NotFound(message)
                is BadRequest -> ApiError.BadRequest(message)
            }
        }
    }

    fun insert(userLocation: UserLocations): JIO<UserLocations> = Jooq.query {
        insertInto(USER_LOCATIONS)
            .set(USER_LOCATIONS.USER_ID, userLocation.userId)
            .set(USER_LOCATIONS.LOCATION_ID, userLocation.locationId)
            .set(USER_LOCATIONS.SENT_HARBOR_NOTIFICATIONS, userLocation.sentHarborNotifications)
            .returning()
            .fetchInto(UserLocations::class.java).first()
    }

    fun update(
        id: Long,
        userLocation: UserLocations
    ): JIO<UserLocations?> = Jooq.query {
        update(USER_LOCATIONS)
            .set(USER_LOCATIONS.USER_ID, userLocation.userId)
            .set(USER_LOCATIONS.LOCATION_ID, userLocation.locationId)
            .set(USER_LOCATIONS.SENT_HARBOR_NOTIFICATIONS, userLocation.sentHarborNotifications)
            .where(USER_LOCATIONS.ID.eq(id))
            .returning()
            .fetchOneInto(UserLocations::class.java)
    }

    fun fetchById(id: Long): JIO<UserLocations?> = Jooq.query {
        selectFrom(USER_LOCATIONS)
            .where(USER_LOCATIONS.ID.eq(id))
            .fetchOneInto(UserLocations::class.java)
    }

    fun fetchAllByUserId(userId: Long): JIO<List<UserLocationDTO?>> = Jooq.query {
        selectFrom(USER_LOCATIONS)
            .where(USER_LOCATIONS.USER_ID.eq(userId))
            .fetchInto(UserLocations::class.java)
            .map { UserLocationDTO.from(it) }
    }

    fun fetchByUserIdAndLocationId(userId: Long, locationId: Long): JIO<UserLocations?> = Jooq.query {
        selectFrom(USER_LOCATIONS)
            .where(USER_LOCATIONS.USER_ID.eq(userId), USER_LOCATIONS.LOCATION_ID.eq(locationId))
            .fetchOneInto(UserLocations::class.java)
    }

    fun fetchAllUserLocationsByLocationId(locationId: Long): JIO<List<UserLocationDTO?>> = Jooq.query {
        selectFrom(USER_LOCATIONS)
            .where(USER_LOCATIONS.LOCATION_ID.eq(locationId))
            .fetchInto(UserLocations::class.java)
            .map { UserLocationDTO.from(it) }
    }

    fun deleteById(id: Long): JIO<Unit> = Jooq.query {
        deleteFrom(USER_LOCATIONS)
            .where(USER_LOCATIONS.ID.eq(id))
            .execute()
    }
}