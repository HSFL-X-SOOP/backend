package hs.flensburg.marlin.business.api.notifications

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.Notification
import java.io.File
import java.io.FileInputStream

class FirebaseNotificationSender {
    private val staticFcmToken = "Here Token vom Gerät ergänzen"
    private val firebaseServiceAccountKeyPath = "Pfad zur Datei angeben"
    private val projectId = "marlin-live-notify"

    init {
        val serviceAccount = FileInputStream(firebaseServiceAccountKeyPath)

        val options = FirebaseOptions.builder()
            .setCredentials(GoogleCredentials.fromStream(serviceAccount))
            .setDatabaseUrl("https://$projectId.firebaseio.com")
            .build()

        // Überprüft, ob die Firebase App bereits initialisiert wurde
        if (FirebaseApp.getApps().isEmpty()) {
            FirebaseApp.initializeApp(options)
            println("Firebase Admin initialisiert.")
        } else {
            println("Firebase Admin bereits initialisiert.")
        }
    }

    fun sendNotification() {
        val notification = Notification.builder()
            .setTitle("Title")
            .setBody("Message")
            .build()

        val message = Message.builder()
            .setToken(staticFcmToken)
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