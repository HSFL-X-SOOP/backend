package hs.flensburg.marlin.business.api.email.control

import de.lambda9.tailwind.core.KIO
import de.lambda9.tailwind.core.extensions.kio.onNullFail
import de.lambda9.tailwind.core.extensions.kio.orDie
import hs.flensburg.marlin.business.ApiError
import hs.flensburg.marlin.business.App
import hs.flensburg.marlin.business.JEnv
import hs.flensburg.marlin.business.ServiceLayerError
import hs.flensburg.marlin.business.api.auth.control.JWTAuthority
import hs.flensburg.marlin.business.api.auth.control.MagicLinkCodeRepo
import hs.flensburg.marlin.business.api.auth.entity.Platform
import hs.flensburg.marlin.business.api.users.control.UserRepo
import hs.flensburg.marlin.database.generated.enums.EmailType
import hs.flensburg.marlin.database.generated.tables.pojos.Email
import hs.flensburg.marlin.database.generated.tables.pojos.UserView
import hs.flensburg.marlin.database.generated.tables.records.MagicLinkCodeRecord
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
    private val logo: ByteArray? by lazy {
        EmailHandler::class.java.getResourceAsStream("/assets/marlin-logo.png")?.use { it.readBytes() }
    }

    private fun loadTemplate(templateName: String): String {
        return templateCache.getOrPut(templateName) {
            EmailHandler::class.java.getResourceAsStream("/email-templates/$templateName.html")
                ?.bufferedReader()
                ?.use { it.readText() }
                ?: throw IllegalStateException("Template $templateName not found")
        }
    }

    fun sendEmail(
        email: Email,
        platform: Platform? = null,
        vararg infoFields: Pair<String, String>
    ): App<Error, Unit> = KIO.comprehension {
        val config = (!KIO.access<JEnv>()).env.config
        val user = !UserRepo.fetchViewById(email.userId!!).orDie().onNullFail { Error.UserNotFound(email.userId!!) }

        val mailBuilder = EmailBuilder.startingBlank()
            .from(config.mail.sendFrom)
            .to(user.email!!)
            .withSubject(subject(email.type!!))
            .withHTMLText(!buildBody(email, user, config.frontendUrl, platform, *infoFields))

        if (logo != null) {
            mailBuilder.withEmbeddedImage("marlin-logo", logo!!, "image/png")
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

    private fun buildBody(
        email: Email,
        user: UserView,
        frontendUrl: String,
        platform: Platform?,
        vararg infoFields: Pair<String, String>
    ): App<Error, String> = KIO.comprehension {
        val baseTemplate = loadTemplate("base")
        val contentTemplate = loadTemplate(templateNameForType(email.type!!, platform))

        val token = when {
            email.type == EmailType.EMAIL_VERIFICATION -> JWTAuthority.generateEmailVerificationToken(user)
            email.type == EmailType.MAGIC_LINK && platform == Platform.MOBILE -> {
                val code = MagicLinkCodeRepo.generateCode()
                val record = MagicLinkCodeRecord().apply {
                    this.userId = user.id
                    this.code = code
                }
                !MagicLinkCodeRepo.insert(record).orDie()
                code
            }

            email.type == EmailType.MAGIC_LINK -> JWTAuthority.generateMagicLinkToken(user)
            else -> ""
        }

        val content = contentTemplate
            .replace("{{token}}", token)
            .replace("{{frontendUrl}}", frontendUrl)
            .replace("{{infoFields}}", infoFields.toList().toInfoListHtml())

        KIO.ok(
            baseTemplate
                .replace("{{subject}}", subject(email.type!!))
                .replace("{{email}}", user.email ?: "")
                .replace("{{frontendUrl}}", frontendUrl)
                .replace("{{content}}", content)
        )
    }

    private fun templateNameForType(type: EmailType, platform: Platform? = null): String = when (type) {
        EmailType.WELCOME -> "welcome"
        EmailType.EMAIL_VERIFICATION -> "email-verification"
        EmailType.MAGIC_LINK -> if (platform == Platform.MOBILE) "magic-link-mobile" else "magic-link"
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