import hs.flensburg.marlin.database.generated.tables.pojos.NotificationLocations
import kotlinx.serialization.Serializable

@Serializable
data class NotificationLocationDTO(
    var id: Long,
    var locationId: Long,
    var notificationTitle: String,
    var notificationText: String,
    var createdBy: Long
) {
    companion object {
        fun from(notificationLocation: NotificationLocations): NotificationLocationDTO {
            return NotificationLocationDTO(
                id = notificationLocation.id!!,
                locationId = notificationLocation.locationId!!,
                notificationTitle = notificationLocation.notificationTitle!!,
                notificationText = notificationLocation.notificationText!!,
                createdBy = notificationLocation.createdBy!!
            )
        }
    }
}