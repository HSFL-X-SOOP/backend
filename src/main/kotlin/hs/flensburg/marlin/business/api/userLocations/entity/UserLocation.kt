import hs.flensburg.marlin.database.generated.tables.pojos.UserLocations
import kotlinx.serialization.Serializable

@Serializable
data class UserLocationDTO(
    var id: Long,
    var userId: Long,
    var locationId: Long,
    var sentHarborNotifications: Boolean
) {
    companion object {
        fun from(userLocation: UserLocations): UserLocationDTO {
            return UserLocationDTO(
                id = userLocation.id!!,
                userId = userLocation.userId!!,
                locationId = userLocation.locationId!!,
                sentHarborNotifications = userLocation.sentHarborNotifications!!
            )
        }
    }
}