# <span style="color:coral"> **GEOMAR x SOOP x HSFL** </span>

---

## 📦 Requirements

- [JDK 21](https://www.oracle.com/de/java/technologies/downloads/#java21)
- [Docker](https://www.docker.com/)
- [Docker Compose](https://docs.docker.com/compose/install/) (not required if using Docker Desktop)
- [Maven](https://maven.apache.org/)

---

# Starting the Backend

Before starting the backend server, make sure to start the database using Docker Compose. Follow the steps below:

1. **Configure Environment Variables**:

   To configure the environment variables for the backend, create a `.env` file in the root directory of the backend
   project and define the necessary variables. For example:

    ```env
    DB_USER=your_db_user
    DB_PASSWORD=your_db_password
    DB_NAME=your_db_name
    ```

   For a complete list of the required environment variables, refer to the `Config` file located in the main directory
   of the backend project.

   > **Note**: The `DB_NAME` should match the name defined in the `docker-compose.yml` file to ensure proper connection
   between the backend and the database.

2. **Start the Database**: Run the following command to start the database in the background:

    ```bash
    docker compose up -d
    ```

   This will initialize the database container. If you're on **Windows**, ensure that **Docker Desktop** is running
   before executing the command.

3. **Start the Server**: The backend server is started by running the `Main.kt` file. Open your project in your
   preferred editor, and run the `Main.kt` file to start the Ktor server.

Once the database is running and the server is started, the backend will be available at the configured host and port (
usually `http://localhost:8080`).

---

## Flyway

Flyway is used for database migration and provides version control for migration scripts. In this project, Flyway is
invoked through Maven to execute the migration scripts.

Migration scripts must be placed in the `src/main/resources/db/migration` folder and named in the format
`V{date}__{name}.sql`, where `{date}` refers to the version number, which uses a date format to determine the migration
order. For example, a migration script might be named `V2025_05_01__initial_schema.sql`.

When adding a new migration script, the date in the filename should be incremented correctly (following the previous one
in chronological order) to ensure that the migrations are applied in the right sequence. The date format should be
continuous, meaning each migration script should have a unique and sequential date to avoid conflicts.

**Important**: Migrations that have been deployed to a production environment cannot be changed. If you need to modify
something, you must create a new migration script. This ensures the integrity of the migration history.

### `afterMigrate.sql` File

In addition to regular migration code, Flyway supports special hooks that allow you to perform tasks at specific points
during the migration process. One such file is the `afterMigrate` file, which runs **after** all migrations are
successfully applied.

This file is a good place to put all your **view creation scripts**, as well as other schema modifications or
post-migration tasks that need to be executed after the main database schema has been updated.

For example, after the database schema migrations are completed (e.g., creating tables or altering columns), you can use
the `afterMigrate` file to create all your views:

```sql
-- File: afterMigrate.sql
-- Creating views after migrations have been applied

CREATE VIEW active_users AS
SELECT id, username
FROM users
WHERE status = 'active';

CREATE VIEW user_summary AS
SELECT id, username, email
FROM users;
```

The key advantage of placing view creation in the `afterMigrate` file is flexibility. Since the `afterMigrate` file is
not tied to migration versioning, you can freely modify, add, or remove views at any time without worrying about the
impact on the migration history or sequence. You can make changes here even after production migrations have been
applied.

Other common Flyway-related scripts may include:

- **`beforeMigrate`**: Runs before the migrations are executed. It's useful for tasks that need to be completed before
  migrations, such as backing up the database or preparing the environment.
- **`afterMigrate`**: Runs after the migrations are completed. This is ideal for tasks like creating views, adding
  indexes, or inserting reference data.

For a full list of Flyway-related scripts and callback events, you can refer to the
official [Flyway callback events documentation](https://documentation.red-gate.com/flyway/reference/callback-events).

Flyway is invoked via Maven to apply the migrations. To run the migrations manually, use the following command:

```bash
mvn flyway:migrate
```

The most important Flyway commands are:

- `mvn flyway:migrate`: Executes the migrations.
- <span style="color: red;">`mvn flyway:clean`: removes the version control and deletes the database. This command
  should be used with
  caution and is disabled in production environments to prevent accidental deletion.
- `mvn flyway:info`: Displays information about the status of migrations.

---

## 🏗 Architecture

### 🌐 API Design

- **Stateless**: The API is designed to be stateless, meaning no session data is stored between requests. Each request
  must carry all necessary context.
- **Authentication**: Authentication will be handled via JWT tokens, with the possibility of integrating OAuth2 in the
  future.
- **Serialization**: All data exchanged through the API is serialized and deserialized using **JSON**.

### 🧱 Business Logic Structure

The project follows a domain-centric folder structure for each business entity. Each entity resides in its own folder
and is organized into three subfolders to encapsulate responsibilities:

```
<entity-name>/
├── boundary/
│   └── Contains public-facing classes and objects (e.g., API models, service interfaces)
├── control/
│   └── Contains core logic such as repositories, background services, and utilities
├── entity/
    └── Contains internal data representations like data classes, DTOs, etc.
```

#### Example:

```
user/
├── boundary/
│   └── UserApi.kt, UserService.kt
├── control/
│   └── UserRepository.kt, UserUtils.kt
├── entity/
    └── UserDto.kt, User.kt
```

This structure encourages clear separation of concerns:

- `boundary`: What the entity exposes to the outside world.
- `control`: What governs the entity's behavior.
- `entity`: What the entity is, in terms of data.

### 🛠 Database Access with Jooq

The project uses [Jooq](https://www.jooq.org/) as the primary DSL for SQL database interaction. To streamline query
execution and error handling, a utility object `Jooq` and a sealed `Result` wrapper class are provided.

### 📦 `Jooq` Object

The `Jooq` object offers two main functions:

- `query { ... }`: Executes a read/write operation using a shared `DSLContext`.
- `transactionalQuery { ... }`: Executes the given query in a transactional context.

Both methods return a `Result<DataAccessException, T>` object, wrapping either the result or a database access error.

```kotlin
// Example usage
val result = Jooq.query {
    selectFrom(USER).where(USER.ID.eq(1)).fetchOne()
}
```

### ✅ `Result` Wrapper

The custom `Result` class is a sealed type representing either:

- `Success<T>` – if the operation was successful
- `Failure<E>` – if an error occurred

This encourages explicit and type-safe error handling in both business logic and data access.

```kotlin
when (result) {
    is Result.Success -> {
        val user = result.result
        // handle success
    }
    is Result.Failure -> {
        val error = result.error
        // handle error
    }
}
```

### 💡 Best Practice

Whenever implementing complex business logic or operations prone to failure, always encapsulate the return values in the
Result type. This ensures that potential failure cases are explicitly handled, and it avoids the pitfalls of unchecked
exceptions. Use Result consistently in all operations where failure is a possibility, making it clear whether the
operation was successful or encountered an error.

Use the `Jooq` utility functions to standardize database interaction, enforce error handling, and support consistent use
of transactions across the codebase.

## Error Handling

**non-trivial business logic** and operations prone to failure are handled by clearly defining
and managing errors in a structured manner. Instead of relying on unchecked exceptions, custom error classes and
interfaces are used to represent specific error scenarios. This approach enhances maintainability, readability, and
debugging, ensuring that the application behaves predictably even when errors occur.

### `ApiError` Class: Defining Specific Errors

The `ApiError` class represents different errors that might arise in the application, each associated with an HTTP
status code and a descriptive message. This class is used throughout the application to provide clear error information
in case of failures.

#### Example: `ApiError` Class

```kotlin
/**
 * The [ApiError] class represents an error that can occur in the application.
 *
 * @param statusCode The HTTP status code associated with the error.
 * @param message A message describing the error.
 */
open class ApiError(
    val statusCode: HttpStatusCode,
    val message: String,
) {
    data class NotFound(val msg: String?) : ApiError(HttpStatusCode.NotFound, msg ?: "Nicht gefunden")
    data class BadRequest(val msg: String?) : ApiError(HttpStatusCode.BadRequest, msg ?: "Fehlerhafte Anfrage")
    data class AlreadyExists(val msg: String?) : ApiError(HttpStatusCode.Conflict, msg ?: "Bereits vorhanden")
    data class Unknown(val msg: String?) : ApiError(HttpStatusCode.InternalServerError, msg ?: "Unbekannter Fehler")

    // Add more error types as needed
}
```

#### Key Error Types:

- **`NotFound`**: Represents a situation where a resource cannot be found (HTTP 404).
- **`BadRequest`**: Used when an invalid request or malformed data is detected (HTTP 400).
- **`AlreadyExists`**: Used when a conflict occurs, such as when a resource already exists (HTTP 409).
- **`Unknown`**: Represents an unforeseen or generic error (HTTP 500).

These error types can be extended based on project needs (e.g., adding errors like `Unauthorized`, `Forbidden`, etc.).

### `ServiceLayerError` Interface: Handling Errors in the Service Layer

In complex applications, errors can occur in the service layer, which is responsible for business logic and interacting
with the database or other external systems. The `ServiceLayerError` interface ensures that errors in the service layer
are consistently transformed into API-friendly error responses.

#### Example: `ServiceLayerError` Interface

```kotlin
/**
 * The [ServiceLayerError] interface represents an error that can occur in the service layer.
 * It provides a method to convert the error to an [ApiError].
 */
interface ServiceLayerError {
    fun toApiError(): ApiError
}
```

Implementing this interface in the service layer allows errors to be seamlessly converted into `ApiError` objects,
ensuring that all errors are handled consistently across the application.

### Conclusion

The structured approach to error handling in this project—using `ApiError` for standardized error representation and
`ServiceLayerError` for error handling in the service layer—improves maintainability and ensures that errors are
communicated effectively to the client. This design makes error handling more transparent, easier to debug, and
consistent throughout the application.

