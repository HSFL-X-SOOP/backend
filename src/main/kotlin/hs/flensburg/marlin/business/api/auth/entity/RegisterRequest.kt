package hs.flensburg.marlin.business.api.auth.entity

import kotlinx.serialization.Serializable

private val EMAIL_REGEX = Regex(
    // RFC-5322 subset: local@domain.tld
    "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
)

/**
 * 8–64 chars, at least one lowercase, one uppercase,
 * one digit and one special char from the allowed set.
 */
private val PASSWORD_REGEX = Regex(
    "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@\$!%*?&-])[A-Za-z\\d@\$!%*?&-]{8,64}$"
)

@Serializable
data class RegisterRequest(val email: String, val password: String, val rememberMe: Boolean) {
    init {
        require(email.isNotBlank()) { "Email must not be blank." }
        require(EMAIL_REGEX.matches(email)) { "Invalid email format." }

        require(password.isNotBlank()) { "Password must not be blank." }
        require(PASSWORD_REGEX.matches(password)) {
            "Password must be 8-64 characters long and include " +
                    "uppercase, lowercase, a digit, and a special character. " +
                    "Allowed special characters are: @, \$, !, %, *, ?, &, -."
        }
    }
}