package hs.flensburg.marlin.business.api.notifications
import hs.flensburg.marlin.business.httpclient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable

object NotificationSender {

    suspend fun sendNotification(expoToken: String, title: String, body: String) {
        val message = ExpoPushMessage(
            to = expoToken,
            title = title,
            body = body
        )

        try {
            val response = httpclient.post("https://exp.host/--/api/v2/push/send") {
                contentType(ContentType.Application.Json)
                setBody(message)
            }

            println("Nachricht erfolgreich gesendet: $response")
        } catch (e: Exception) {
            System.err.println("Fehler beim Senden der Nachricht: ${e.message}")
            e.printStackTrace()
        }
    }
}

@Serializable
data class ExpoPushMessage(
    val to: String,
    val title: String,
    val body: String,
    val data: Map<String, String>? = null
)