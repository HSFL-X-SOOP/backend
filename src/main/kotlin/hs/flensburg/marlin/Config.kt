package hs.flensburg.marlin

import io.github.cdimascio.dotenv.Dotenv

data class Config(
    val mode: Mode,
    val http: Http,
    val database: Database,
    val mail: Mail,
    val auth: Auth,
    val googleAuth: GoogleAuth,
    val appleAuth: AppleAuth,
    val ipInfo: IPInfo,
    val firebaseInfo: FirebaseInfo,
    val stripe: Stripe,
    val dataSources: DataSources
) {
    val frontendUrl: String
        get() = when (mode) {
            Mode.DEV -> "http://localhost:8081"
            Mode.STAGING -> "https://test.marlin-live.com"
            Mode.PROD -> "https://marlin-live.com"
        }

    val backendUrl: String
        get() = when (mode) {
            Mode.DEV -> "http://localhost:${http.port}"
            Mode.STAGING -> "https://test.marlin-live.com/api"
            Mode.PROD -> "https://marlin-live.com/api"
        }

    enum class Mode {
        DEV, // Mode for local development
        STAGING, // Mode for staging environment
        PROD, // Mode for production environment
    }

    data class Http(
        val host: String,
        val port: Int,
    )

    data class Database(
        val url: String,
        val user: String,
        val password: String,
    )

    data class Mail(
        val host: String,
        val port: Int,
        val username: String,
        val password: String,
        val sendFrom: String
    )

    data class Auth(
        val jwtSecret: String,
        val jwtIssuer: String,
        val jwtAudience: String,
    )

    data class GoogleAuth(
        val clientId: String,
        val clientSecret: String,
        val iosClientId: String,
    )

    data class AppleAuth(
        val clientId: String,
    )

    data class IPInfo(
        val token: String
    )

    data class FirebaseInfo(
        val firebaseServiceAccountKeyPath: String,
        val firebaseCloudMessagingProjectID: String
    )

    data class Stripe(
        val secretKey: String,
        val publishableKey: String,
        val webhookSecret: String,
        val notificationPriceId: String,
        val apiAccessPriceId: String,
        val trialDays: Int
    )

    data class DataSources(
        val FrostServerPath: String
    )

    companion object {
        fun Dotenv.parseConfig(): Config = Config(
            mode = Mode.valueOf(get("MODE", "DEV").uppercase()),
            http = Http(
                host = get("HTTP_HOST", "0.0.0.0"),
                port = get("HTTP_PORT", "8080").toInt(),
            ),
            database = Database(
                url = get("DATABASE_URL", "jdbc:postgresql://localhost:6000/marlin"),
                user = get("DATABASE_USER", "user"),
                password = get("DATABASE_PASSWORD", "sql"),
            ),
            mail = Mail(
                host = get("MAIL_HOST", ""),
                port = get("MAIL_PORT", "").toInt(),
                username = get("MAIL_USERNAME", ""),
                password = get("MAIL_PASSWORD", ""),
                sendFrom = get("MAIL_FROM", "")
            ),
            auth = Auth(
                jwtSecret = get("JWT_SECRET_ACCESS", ""),
                jwtIssuer = get("JWT_ISSUER", ""),
                jwtAudience = get("JWT_AUDIENCE", "")
            ),
            googleAuth = GoogleAuth(
                clientId = get("GOOGLE_CLIENT_ID", ""),
                clientSecret = get("GOOGLE_CLIENT_SECRET", ""),
                iosClientId = get("GOOGLE_IOS_CLIENT_ID", "")
            ),
            appleAuth = AppleAuth(
                clientId = get("APPLE_CLIENT_ID", "")
            ),
            ipInfo = IPInfo(
                token = get("IPINFO_TOKEN", "")
            ),
            firebaseInfo = FirebaseInfo(
                firebaseServiceAccountKeyPath = get("FIREBASE_SERVICE_ACCOUNT_KEY_PATH", ""),
                firebaseCloudMessagingProjectID = get("FIREBASE_CLOUD_PROJECT_ID", "")
            ),
            stripe = Stripe(
                secretKey = get("STRIPE_SECRET_KEY", ""),
                publishableKey = get("STRIPE_PUBLISHABLE_KEY", ""),
                webhookSecret = get("STRIPE_WEBHOOK_SECRET", ""),
                notificationPriceId = get("STRIPE_NOTIFICATION_PRICE_ID", ""),
                apiAccessPriceId = get("STRIPE_API_ACCESS_PRICE_ID", ""),
                trialDays = get("STRIPE_TRIAL_DAYS", "14").toInt()
            ),
            dataSources = DataSources(
                FrostServerPath = get("FROST_SERVER_PATH", "")
            )
        )

        fun parseConfig(): Config = Config(
            mode = Mode.valueOf((System.getenv("MODE") ?: "DEV").uppercase()),
            http = Http(
                host = System.getenv("HTTP_HOST") ?: "0.0.0.0",
                port = (System.getenv("HTTP_PORT") ?: "8080").toInt(),
            ),
            database = Database(
                url = System.getenv("DATABASE_URL") ?: "jdbc:postgresql://localhost:6000/marlin",
                user = System.getenv("DATABASE_USER") ?: "user",
                password = System.getenv("DATABASE_PASSWORD") ?: "sql",
            ),
            mail = Mail(
                host = System.getenv("MAIL_HOST") ?: "",
                port = (System.getenv("MAIL_PORT") ?: "").toInt(),
                username = System.getenv("MAIL_USERNAME") ?: "",
                password = System.getenv("MAIL_PASSWORD") ?: "",
                sendFrom = System.getenv("MAIL_FROM") ?: ""
            ),
            auth = Auth(
                jwtSecret = System.getenv("JWT_SECRET_ACCESS") ?: "",
                jwtIssuer = System.getenv("JWT_ISSUER") ?: "",
                jwtAudience = System.getenv("JWT_AUDIENCE") ?: ""
            ),
            googleAuth = GoogleAuth(
                clientId = System.getenv("GOOGLE_CLIENT_ID") ?: "",
                clientSecret = System.getenv("GOOGLE_CLIENT_SECRET") ?: "",
                iosClientId = System.getenv("GOOGLE_IOS_CLIENT_ID") ?: ""
            ),
            appleAuth = AppleAuth(
                clientId = System.getenv("APPLE_CLIENT_ID") ?: ""
            ),
            ipInfo = IPInfo(
                token = System.getenv("IPINFO_TOKEN") ?: ""
            ),
            firebaseInfo = FirebaseInfo(
                firebaseServiceAccountKeyPath = System.getenv("FIREBASE_SERVICE_ACCOUNT_KEY_PATH") ?: "",
                firebaseCloudMessagingProjectID = System.getenv("FIREBASE_CLOUD_PROJECT_ID") ?: ""
            ),
            stripe = Stripe(
                secretKey = System.getenv("STRIPE_SECRET_KEY") ?: "",
                publishableKey = System.getenv("STRIPE_PUBLISHABLE_KEY") ?: "",
                webhookSecret = System.getenv("STRIPE_WEBHOOK_SECRET") ?: "",
                notificationPriceId = System.getenv("STRIPE_NOTIFICATION_PRICE_ID") ?: "",
                apiAccessPriceId = System.getenv("STRIPE_API_ACCESS_PRICE_ID") ?: "",
                trialDays = (System.getenv("STRIPE_TRIAL_DAYS") ?: "14").toInt()
            ),
            dataSources = DataSources(
                FrostServerPath = System.getenv("FROST_SERVER_PATH") ?: ""
            )
        )
    }
}