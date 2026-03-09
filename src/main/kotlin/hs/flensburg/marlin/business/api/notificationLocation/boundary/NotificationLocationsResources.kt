import de.lambda9.tailwind.core.KIO.Companion.unsafeRunSync
import hs.flensburg.marlin.business.api.notificationLocation.entity.CreateOrUpdateNotificationLocationRequest
import hs.flensburg.marlin.business.api.notifications.NotificationSender
import hs.flensburg.marlin.business.api.openAPI.NotificationLocationOpenAPISpec
import hs.flensburg.marlin.business.api.userDevice.control.UserDeviceRepo
import hs.flensburg.marlin.business.api.userDevice.entity.UserDevice
import hs.flensburg.marlin.database.generated.tables.pojos.UserLocations
import hs.flensburg.marlin.plugins.kioEnv
import hs.flensburg.marlin.plugins.respondKIO
import io.github.smiley4.ktoropenapi.delete
import io.github.smiley4.ktoropenapi.get
import io.github.smiley4.ktoropenapi.post
import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.routing.routing

fun Application.configureNotificationLocations() {
    routing {
        get("/notification-locations/{id}", NotificationLocationOpenAPISpec.getNotificationLocation) {
            val id = call.parameters["id"]!!.toLong()
            call.respondKIO(NotificationLocationsService.getNotificationLocation(id))
        }

        get("/notification-locations/all/{locationId}", NotificationLocationOpenAPISpec.getAllNotificationsLocation) {
            val locationId = call.parameters["locationId"]!!.toLong()
            call.respondKIO(NotificationLocationsService.getAllNotificationLocationsFromLocation(locationId))
        }

        post("/notification-locations", NotificationLocationOpenAPISpec.postNotificationLocation) {
            val request = call.receive<CreateOrUpdateNotificationLocationRequest>()

            var allUserLocations: List<UserLocations?> =
                UserLocationsRepo.fetchAllUserLocationsByLocationId(request.locationId).unsafeRunSync(call.kioEnv).fold(
                    onError = { listOf<UserLocations>() },
                    onSuccess = { it }
                )
            allUserLocations =
                allUserLocations.filter { userLocations -> userLocations!!.sentHarborNotifications == true }
            allUserLocations.forEach { userLocation ->
                val userId: Long = userLocation!!.userId!!
                val allUserDevices: List<UserDevice?> =
                    UserDeviceRepo.fetchAllByUserId(userId).unsafeRunSync(call.kioEnv).fold(
                        onError = { listOf<UserDevice>() },
                        onSuccess = { it }
                    )
                allUserDevices.forEach { device ->
                    println("send noti")
                    NotificationSender.sendNotification(
                        expoToken = device!!.fcmToken,
                        title = request.notificationTitle,
                        body = request.notificationText
                    )
                }
            }

            call.respondKIO(NotificationLocationsService.create(request))
        }

        delete("/notification-locations/{id}", NotificationLocationOpenAPISpec.deleteNotificationLocation) {
            val id = call.parameters["id"]!!.toLong()
            call.respondKIO(NotificationLocationsService.delete(id))
        }
    }
}