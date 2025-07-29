package hs.flensburg.marlin

import io.github.cdimascio.dotenv.Dotenv

data class Config(
    val http: Http,
    val database: Database,
    val mode: Mode,
    val auth: Auth
) {

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

    data class Auth(
        val jwtSecretAccess: String,
        val jwtSecretRefresh: String,
        val jwtIssuer: String,
        val jwtAudience: String,
    )

    companion object {
        fun Dotenv.parseConfig(): Config = Config(
            http = Http(
                host = get("HTTP_HOST", "0.0.0.0"),
                port = get("HTTP_PORT", "8080").toInt(),
            ),
            database = Database(
                url = get("DATABASE_URL", "jdbc:postgresql://localhost:6000/marlin"),
                user = get("DATABASE_USER", "user"),
                password = get("DATABASE_PASSWORD", "sql"),
            ),
            mode = Mode.valueOf(get("MODE", "DEV").uppercase()),
            auth = Auth(
                jwtSecretAccess = get("JWT_SECRET_ACCESS", ""),
                jwtSecretRefresh = get("JWT_SECRET_REFRESH", ""),
                jwtIssuer = get("JWT_ISSUER", ""),
                jwtAudience = get("JWT_AUDIENCE", "")
            )
        )

        fun parseConfig(): Config = Config(
            http = Http(
                host = System.getenv("HTTP_HOST") ?: "0.0.0.0",
                port = (System.getenv("HTTP_PORT") ?: "8080").toInt(),
            ),
            database = Database(
                url = System.getenv("DATABASE_URL") ?: "jdbc:postgresql://localhost:6000/marlin",
                user = System.getenv("DATABASE_USER") ?: "user",
                password = System.getenv("DATABASE_PASSWORD") ?: "sql",
            ),
            mode = Mode.valueOf((System.getenv("MODE") ?: "DEV").uppercase()),
            auth = Auth(
                jwtSecretAccess = System.getenv("JWT_SECRET_ACCESS") ?: "",
                jwtSecretRefresh = System.getenv("JWT_SECRET_REFRESH") ?: "",
                jwtIssuer = System.getenv("JWT_ISSUER") ?: "",
                jwtAudience = System.getenv("JWT_AUDIENCE") ?: ""
            )
        )
    }
}