package hs.flensburg.marlin.business.api.openAPI

import hs.flensburg.marlin.business.api.auth.entity.GoogleLoginRequest
import hs.flensburg.marlin.business.api.auth.entity.LoginRequest
import hs.flensburg.marlin.business.api.auth.entity.LoginResponse
import hs.flensburg.marlin.business.api.auth.entity.MagicLinkLoginRequest
import hs.flensburg.marlin.business.api.auth.entity.MagicLinkRequest
import hs.flensburg.marlin.business.api.auth.entity.RefreshTokenRequest
import hs.flensburg.marlin.business.api.auth.entity.RegisterRequest
import hs.flensburg.marlin.business.api.auth.entity.VerifyEmailRequest
import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.ktor.http.HttpStatusCode


object AuthOpenAPISpec {

    val loginGoogle: RouteConfig.() -> Unit = {
        tags("auth")
        description = "Redirects the user to Google's OAuth consent screen for authentication. " +
                "After successful authentication, user will be redirected to /auth/google/callback."

        securitySchemeNames("OAuth2Google")

        response {
            HttpStatusCode.Found to {
                description = "Redirect to Google OAuth consent screen"
            }
        }
    }

    val register: RouteConfig.() -> Unit = {
        tags("auth")
        description = "Creates a new user account with the provided email and password. " +
                "Returns JWT access and refresh tokens upon successful registration."

        request {
            body<RegisterRequest>()
        }

        response {
            HttpStatusCode.Created to {
                description = "User successfully registered"
                body<LoginResponse>()
            }
            HttpStatusCode.BadRequest to {
                description = "Invalid registration data (e.g., weak password, invalid email)"
                body<String>()
            }
            HttpStatusCode.Conflict to {
                description = "User with this email already exists"
                body<String>()
            }
        }
    }

    val login: RouteConfig.() -> Unit = {
        tags("auth")
        description = "Authenticates a user using email and password credentials. " +
                "Returns JWT access and refresh tokens upon successful authentication. " +
                "May redirect to Google OAuth if the account was created via Google."

        request {
            body<LoginRequest>()
        }

        response {
            HttpStatusCode.OK to {
                description = "Login successful"
                body<LoginResponse>()
            }
            HttpStatusCode.Unauthorized to {
                description = "Invalid credentials or unverified email"
                body<String>()
            }
            HttpStatusCode.TooManyRequests to {
                description = "Too many failed login attempts from this IP address"
                body<String>()
            }
            HttpStatusCode.Found to {
                description = "Account created via Google OAuth - redirect to /login/google required"
            }
        }
    }

    val loginGoogleAndroid: RouteConfig.() -> Unit = {
        tags("auth")
        description = "Authenticates a user using a Google ID token obtained from the Android Google Sign-In SDK. " +
                "This endpoint is specifically for mobile applications."

        request {
            body<GoogleLoginRequest>()
        }

        response {
            HttpStatusCode.OK to {
                description = "Login successful"
                body<LoginResponse>()
            }
            HttpStatusCode.BadRequest to {
                description = "Invalid or expired Google ID token"
                body<String>()
            }
        }
    }

    val refreshToken: RouteConfig.() -> Unit = {
        tags("auth")
        description = "Exchanges a valid refresh token for a new access token and refresh token pair. " +
                "Use this endpoint before the access token expires to maintain user session."

        request {
            body<RefreshTokenRequest>()
        }

        response {
            HttpStatusCode.OK to {
                description = "Token refresh successful"
                body<LoginResponse>()
            }
            HttpStatusCode.Unauthorized to {
                description = "Invalid or expired refresh token"
                body<String>()
            }
        }
    }

    val requestMagicLink: RouteConfig.() -> Unit = {
        tags("auth")
        description = "Sends an email containing a magic link that allows the user to log in without a password. " +
                "The link is valid for a limited time."

        request {
            body<MagicLinkRequest>()
        }

        response {
            HttpStatusCode.OK to {
                description = "Magic link email sent (always returns success even if email doesn't exist for security)"
                body<Unit>()
            }
        }
    }

    val loginViaMagicLink: RouteConfig.() -> Unit = {
        tags("auth")
        description = "Authenticates a user using the token from a magic link email. " +
                "Returns JWT access and refresh tokens upon successful authentication."

        request {
            body<MagicLinkLoginRequest>()
        }

        response {
            HttpStatusCode.OK to {
                description = "Magic link login successful"
                body<LoginResponse>()
            }
            HttpStatusCode.Unauthorized to {
                description = "Invalid or expired magic link token"
                body<String>()
            }
        }
    }

    val verifyEmail: RouteConfig.() -> Unit = {
        tags("auth")
        description = "Verifies a user's email address using the token sent via verification email. " +
                "Email verification may be required before certain features are accessible."

        request {
            body<VerifyEmailRequest>()
        }

        response {
            HttpStatusCode.OK to {
                description = "Email successfully verified"
                body<Unit>()
            }
            HttpStatusCode.BadRequest to {
                description = "Invalid or expired verification token"
                body<String>()
            }
        }
    }

    val sendVerificationEmail: RouteConfig.() -> Unit = {
        tags("auth")
        description = "Sends a verification email to the authenticated user's email address. " +
                "Requires a valid JWT access token in the Authorization header."

        securitySchemeNames("BearerAuth")

        response {
            HttpStatusCode.OK to {
                description = "Verification email sent successfully"
                body<Unit>()
            }
            HttpStatusCode.Unauthorized to {
                description = "Missing or invalid JWT token"
                body<String>()
            }
        }
    }
}
