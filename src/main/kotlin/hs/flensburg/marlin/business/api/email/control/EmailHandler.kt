package hs.flensburg.marlin.business.api.email.control

import de.lambda9.tailwind.core.KIO
import de.lambda9.tailwind.core.KIO.Companion.unit
import de.lambda9.tailwind.core.extensions.kio.onNullFail
import de.lambda9.tailwind.core.extensions.kio.orDie
import de.lambda9.tailwind.core.extensions.kio.run
import de.lambda9.tailwind.jooq.transact
import hs.flensburg.marlin.business.ApiError
import hs.flensburg.marlin.business.App
import hs.flensburg.marlin.business.JEnv
import hs.flensburg.marlin.business.ServiceLayerError
import hs.flensburg.marlin.business.api.auth.control.AuthRepo
import hs.flensburg.marlin.business.api.auth.control.JWTAuthority
import hs.flensburg.marlin.business.api.users.control.UserRepo
import hs.flensburg.marlin.database.generated.enums.EmailType
import hs.flensburg.marlin.database.generated.tables.pojos.Email
import hs.flensburg.marlin.database.generated.tables.pojos.User
import org.simplejavamail.api.mailer.config.TransportStrategy
import org.simplejavamail.email.EmailBuilder
import org.simplejavamail.mailer.MailerBuilder

object EmailHandler {
    sealed class Error(private val message: String) : ServiceLayerError {
        data class UserNotFound(val userId: Long) : Error("User with ID $userId not found")

        override fun toApiError(): ApiError {
            return when (this) {
                is UserNotFound -> ApiError.NotFound(message)
            }
        }
    }

    fun sendEmail(email: Email, vararg infoFields: Pair<String, String>): App<Error, Unit> = KIO.comprehension {
        val config = (!KIO.access<JEnv>()).component2().config
        val user = !UserRepo.fetchById(email.userId!!).orDie().onNullFail { Error.UserNotFound(email.userId!!) }

        val mail = EmailBuilder.startingBlank()
            .from(config.mail.sendFrom)
            .to(user.email!!)
            .withSubject(subject(email.type!!))
            .withHTMLText(body(email, user, *infoFields))
            .buildEmail()

        val mailer = MailerBuilder
            .withSMTPServer(
                config.mail.host,
                config.mail.port,
                config.mail.username,
                config.mail.password
            )
            .withTransportStrategy(TransportStrategy.SMTP_TLS)
            .buildMailer()

        !EmailRepo.updateSentAt(email.id!!).orDie()

        mailer.sendMail(mail)

        unit
    }

    private fun subject(type: EmailType): String {
        return when (type) {
            EmailType.WELCOME -> "Welcome to Marlin"
            EmailType.EMAIL_VERIFICATION -> "Marlin - Email Verification"
            EmailType.MAGIC_LINK -> "Marlin - Magic Link Login"
            EmailType.TOO_MANY_FAILED_LOGIN_ATTEMPTS -> "Marlin - Too Many Failed Login Attempts"
        }
    }

    private fun body(email: Email, user: User, vararg infoFields: Pair<String, String>): String = """
        <!DOCTYPE html>
        <html lang="en">
        <head>
          <meta charset="UTF-8">
          <title>${subject(email.type!!)}</title>
        
          <link href="https://fonts.googleapis.com/css2?family=Montserrat:wght@400;600&display=swap" rel="stylesheet">
        
          <style>
            body{
              margin:0;
              padding:0;
              font-family:'Montserrat',Arial,sans-serif;
              background:#f4f4f5;
              color:#000000;
            }
            .wrapper{
              max-width:560px;
              margin:auto;
              background:#ffffff;
              padding:32px;
              border-radius:8px;
              border:1px solid #d4d4d8;
            }
            h1{
              margin-top:0;
              font-size:24px;
              color:#053246;
            }
            p{
              line-height:1.55;
            }
            a{
              color:#053246;
            }
            a.button{
              display:inline-block;
              margin-top:24px;
              padding:12px 24px;
              border-radius:4px;
              text-decoration:none;
              background:#78d278;
              color:#000000;
              font-weight:600;
            }
            small{
              color:#8a8a8c;
            }
        
            @media (prefers-color-scheme: dark) {
              body{
                background:#000000;
                color:#ffffff;
              }
              .wrapper{
                background:#18181b;
                border:1px solid #3f3f46;
              }
              h1{
                color:#eef9ee;
              }
              a{
                color:#78d278;
              }
              a.button{
                background:#78d278;
                color:#000000;
              }
              small{
                color:#8c8c90;
              }
            }
          </style>
        </head>
        
        <body>
          <div class="wrapper">
            <h1>Hello ${user.email}</h1>
            ${message(email.type!!, user, *infoFields)}
          </div>
        </body>
        </html>
        """.trimIndent()

    private fun message(type: EmailType, user: User, vararg infoFields: Pair<String, String>): String = when (type) {
        EmailType.WELCOME -> """
            <p>Thanks for joining <strong>Marlin</strong>—we’re happy you’re here!</p>
            
            <p>
              <a
                class="button"
                href="https://marlin-live.com"
                style="
                  display:inline-block;
                  padding:12px 24px;
                  font-size:16px;
                  line-height:20px;
                  text-decoration:none;
                  color:#fff;
                  background-color:#2563eb;
                  border-radius:6px;
                "
              >Go to your dashboard</a>
            </p>
            
            ${infoFields.toList().toInfoListHtml()}
            
            <p style="font-size:0.875rem;color:#64748b;">
              Need help? Just reply to this email and we’ll get back to you.
            </p>
        """.trimIndent()

        EmailType.EMAIL_VERIFICATION -> """
            <p>Please verify your email address to activate your account:</p>
            
            <p>
              <a
                class="button"
                href="https://marlin-live.com/api/verify?token=${JWTAuthority.generateEmailVerificationToken(user)}"
                style="
                  display:inline-block;
                  padding:12px 24px;
                  font-size:16px;
                  line-height:20px;
                  text-decoration:none;
                  color:#fff;
                  background-color:#16a34a;
                  border-radius:6px;
                "
              >Verify now</a>
            </p>
            
            ${infoFields.toList().toInfoListHtml()}
            
            <p style="font-size:0.875rem;color:#64748b;">
              If you didn’t create an account with Marlin, you can safely ignore this message.
            </p>
        """.trimIndent()

        EmailType.MAGIC_LINK -> """
            <p>Click the button below to sign in to your account:</p>
            
            <p>
              <a
                class="button"
                href="https://marlin-live.com/api/magic-link?token=${JWTAuthority.generateMagicLinkToken(user)}"
                style="
                  display: inline-block;
                  padding: 12px 24px;
                  font-size: 16px;
                  line-height: 20px;
                  text-decoration: none;
                  color: #fff;
                  background-color: #2563eb;
                  border-radius: 6px;
                "
              >Log in</a>
            </p>
            
            <p>This link will expire in <strong>30&nbsp;minutes</strong>.</p>
            
            ${infoFields.toList().toInfoListHtml()}
            
            <p style="font-size: 0.875rem; color: #64748b;">
              If you didn’t request this email, you can safely ignore it.
            </p>
        """.trimIndent()

        EmailType.TOO_MANY_FAILED_LOGIN_ATTEMPTS -> """
            <p>We’ve detected several failed login attempts on your account.</p>
            <p>As a precaution, we’ve temporarily blocked further password logins.</p>
            
            ${infoFields.toList().toInfoListHtml()}
            
            <p>You can still get in right away by either:</p>
            <ul>
              <li>requesting a <strong>magic-link</strong>, or</li>
              <li>signing in with your <strong>Google account</strong>.</li>
            </ul>
            
            <p>If you weren’t trying to log in, you don’t need to do anything, your account remains secure.</p>
            <p>Questions? Reply to this email and our team will help.</p>
        """.trimIndent()
    }

    private fun List<Pair<String, String>>.toInfoListHtml(): String =
        if (isEmpty()) ""
        else buildString {
            appendLine("<ul style=\"margin:1em 0 0 0;padding:0 0 0 1.25em;\">")
            for ((label, value) in this@toInfoListHtml) {
                appendLine(
                    "<li><strong>${label.escape()}</strong>: ${value.escape()}</li>"
                )
            }
            appendLine("</ul>")
        }

    private fun String.escape(): String =
        replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
}