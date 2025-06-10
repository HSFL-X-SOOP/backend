package hs.flensburg.soop

import io.github.cdimascio.dotenv.Dotenv

data class Config(
    val http: Http,
    val database: Database,
    val mode: Mode
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

    companion object {
        fun Dotenv.parseConfig(): Config = Config(
            http = Http(
                host = get("HTTP_HOST", "0.0.0.0"),
                port = get("HTTP_PORT", "8080").toInt(),
            ),
            database = Database(
                url = get("DATABASE_URL", "jdbc:postgresql://localhost:6000/soop"),
                user = get("DATABASE_USER", "user"),
                password = get("DATABASE_PASSWORD", "sql"),
            ),
            mode = Mode.valueOf(get("MODE", "DEV").uppercase()),
        )

        fun parseConfig(): Config = Config(
            http = Http(
                host = System.getenv("HTTP_HOST") ?: "0.0.0.0",
                port = (System.getenv("HTTP_PORT") ?: "8080").toInt(),
            ),
            database = Database(
                url = System.getenv("DATABASE_URL") ?: "jdbc:postgresql://localhost:6000/soop",
                user = System.getenv("DATABASE_USER") ?: "user",
                password = System.getenv("DATABASE_PASSWORD") ?: "sql",
            ),
            mode = Mode.valueOf(
                (System.getenv("MODE") ?: "DEV").uppercase()
            )
        )
    }
}