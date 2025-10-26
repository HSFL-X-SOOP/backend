package hs.flensburg.marlin.business.api.location.boundary

import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receiveMultipart
import io.ktor.utils.io.readRemaining
import kotlinx.io.readByteArray

suspend fun ApplicationCall.receiveImageFile(): ByteArray {
    var imageBytes: ByteArray? = null
    var contentType: String?

    val multipart = receiveMultipart()
    multipart.forEachPart { part ->
        if (part is PartData.FileItem && part.name == "image") {
            contentType = part.contentType?.toString()

            if (contentType?.startsWith("image/") != true) {
                part.dispose()
                throw UnsupportedMediaTypeException("Only image files are allowed (received: $contentType)")
            }

            imageBytes = part.provider().readRemaining().readByteArray()
        }
        part.dispose()
    }

    if (imageBytes == null) {
        throw BadRequestException("No image file provided")
    }

    return imageBytes
}

class BadRequestException(message: String) : RuntimeException(message)
class UnsupportedMediaTypeException(message: String) : RuntimeException(message)