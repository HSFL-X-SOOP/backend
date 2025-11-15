package hs.flensburg.marlin.business.api.email.control

import de.lambda9.tailwind.core.KIO
import de.lambda9.tailwind.core.extensions.kio.onNullFail
import de.lambda9.tailwind.core.extensions.kio.orDie
import hs.flensburg.marlin.business.ApiError
import hs.flensburg.marlin.business.App
import hs.flensburg.marlin.business.JEnv
import hs.flensburg.marlin.business.ServiceLayerError
import hs.flensburg.marlin.business.api.auth.control.JWTAuthority
import hs.flensburg.marlin.business.api.users.control.UserRepo
import hs.flensburg.marlin.database.generated.enums.EmailType
import hs.flensburg.marlin.database.generated.tables.pojos.Email
import hs.flensburg.marlin.database.generated.tables.pojos.UserView
import org.simplejavamail.api.mailer.config.TransportStrategy
import org.simplejavamail.email.EmailBuilder
import org.simplejavamail.mailer.MailerBuilder

object EmailHandler {
    sealed class Error(private val message: String) : ServiceLayerError {
        data class UserNotFound(val userId: Long) : Error("User with ID $userId not found")
        data class TemplateNotFound(val templateName: String) : Error("Email template '$templateName' not found")

        override fun toApiError(): ApiError {
            return when (this) {
                is UserNotFound -> ApiError.NotFound(message)
                is TemplateNotFound -> ApiError.Unknown(message)
            }
        }
    }

    private val templateCache = mutableMapOf<String, String>()

    private fun loadTemplate(templateName: String): String {
        return templateCache.getOrPut(templateName) {
            EmailHandler::class.java.getResourceAsStream("/email-templates/$templateName.html")
                ?.bufferedReader()
                ?.use { it.readText() }
                ?: throw IllegalStateException("Template $templateName not found")
        }
    }

    fun sendEmail(email: Email, vararg infoFields: Pair<String, String>): App<Error, Unit> = KIO.comprehension {
        val config = (!KIO.access<JEnv>()).env.config
        val user = !UserRepo.fetchViewById(email.userId!!).orDie().onNullFail { Error.UserNotFound(email.userId!!) }

        val logoStream = EmailHandler::class.java.getResourceAsStream("/assets/marlin-logo.png")

        val mailBuilder = EmailBuilder.startingBlank()
            .from(config.mail.sendFrom)
            .to(user.email!!)
            .withSubject(subject(email.type!!))
            .withHTMLText(body(email, user, *infoFields))

        if (logoStream != null) {
            mailBuilder.withEmbeddedImage("marlin-logo", logoStream.readBytes(), "image/png")
        }

        val mail = mailBuilder.buildEmail()

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

        KIO.unit
    }

    private fun subject(type: EmailType): String {
        return when (type) {
            EmailType.WELCOME -> "Welcome to Marlin"
            EmailType.EMAIL_VERIFICATION -> "Marlin - Email Verification"
            EmailType.MAGIC_LINK -> "Marlin - Magic Link Login"
            EmailType.TOO_MANY_FAILED_LOGIN_ATTEMPTS -> "Marlin - Suspicious Activity"
        }
    }

    private fun body(email: Email, user: UserView, vararg infoFields: Pair<String, String>): String {
        val baseTemplate = loadTemplate("base")
        val contentTemplate = loadTemplate(templateNameForType(email.type!!))

        val token = when (email.type) {
            EmailType.EMAIL_VERIFICATION -> JWTAuthority.generateEmailVerificationToken(user)
            EmailType.MAGIC_LINK -> JWTAuthority.generateMagicLinkToken(user)
            else -> ""
        }

        val content = contentTemplate
            .replace("{{token}}", token)
            .replace("{{infoFields}}", infoFields.toList().toInfoListHtml())

        return baseTemplate
            .replace("{{subject}}", subject(email.type!!))
            .replace("{{email}}", user.email ?: "")
            .replace("{{content}}", content)
    }

    private fun templateNameForType(type: EmailType): String = when (type) {
        EmailType.WELCOME -> "welcome"
        EmailType.EMAIL_VERIFICATION -> "email-verification"
        EmailType.MAGIC_LINK -> "magic-link"
        EmailType.TOO_MANY_FAILED_LOGIN_ATTEMPTS -> "too-many-failed-login-attempts"
    }

    private fun List<Pair<String, String>>.toInfoListHtml(): String =
        if (isEmpty()) ""
        else buildString {
            appendLine("<hr style=\"border: none; border-top: 1px solid #52525b; margin: 24px 0;\">")
            appendLine("<ul style=\"margin: 16px 0 0 0; padding: 0 0 0 20px; font-family: 'Oswald', sans-serif; font-size: 14px; line-height: 1.6;\">")
            for ((label, value) in this@toInfoListHtml) {
                appendLine(
                    "<li style=\"margin-bottom: 8px; color: #f0f0f0;\"><strong style=\"color: #f0f0f0;\">${label.escape()}</strong>: <span style=\"color: #5fc0fc;\">${value.escape()}</span></li>"
                )
            }
            appendLine("</ul>")
        }

    private fun String.escape(): String =
        replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
}