package hs.flensburg.marlin.business.api.notifications

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.Notification
import hs.flensburg.marlin.Config
import io.ktor.server.application.Application
import java.io.FileInputStream

fun Application.configureFirebase(config: Config.FirebaseInfo) {
    val serviceAccount = FileInputStream(config.firebaseServiceAccountKeyPath)

    val options = FirebaseOptions.builder()
        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
        .setDatabaseUrl("https://${config.firebaseCloudMessagingProjectID}.firebaseio.com")
        .build()

    FirebaseApp.initializeApp(options)
}

object FirebaseNotificationSender {

    fun sendNotification(token: String) {
        val notification = Notification.builder()
            .setTitle("Title")
            .setBody("Message")
            .build()

        val message = Message.builder()
            .setToken(token)
            .setNotification(notification)
            // .putData("key1", "value1") falls daten mitgegeben werden müssen, kann man diese anhängen
            .build()

        try {
            val response = FirebaseMessaging.getInstance().send(message)
            println("Nachricht erfolgreich gesendet: $response")
        } catch (e: Exception) {
            System.err.println("Fehler beim Senden der Nachricht: ${e.message}")
            e.printStackTrace()
        }
    }
}