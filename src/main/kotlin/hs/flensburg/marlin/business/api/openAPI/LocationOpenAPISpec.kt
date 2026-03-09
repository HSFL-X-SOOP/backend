package hs.flensburg.marlin.business.api.openAPI

import hs.flensburg.marlin.business.api.location.entity.Contact
import hs.flensburg.marlin.business.api.location.entity.DetailedLocationDTO
import hs.flensburg.marlin.business.api.location.entity.ImageRequest
import hs.flensburg.marlin.business.api.location.entity.UpdateLocationRequest
import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData

object LocationOpenAPISpec {

    val getLocation: RouteConfig.() -> Unit = {
        tags("location")
        description = "Get a location"
        request {
            pathParameter<Long>("id") {
                description = "The location ID (not the sensor ID)"
            }
            queryParameter<String>("timezone") {
                description =
                    "Optional timezone ('Europe/Berlin'). Defaults to Ip address based timezone. Backup UTC."
                required = false
            }
        }
        response {
            HttpStatusCode.OK to {
                description = "Successful response with measurements"
                body<DetailedLocationDTO>()
            }
            HttpStatusCode.BadRequest to {
                description = "Invalid parameters"
            }
        }
    }

    val getLocationImage: RouteConfig.() -> Unit = {
        tags("location")
        description = "Get a location image"
        request {
            pathParameter<Long>("id") {
                description = "The location ID (not the sensor ID)"
            }
        }
        response {
            HttpStatusCode.OK to {
                description = "Successful response with image"
                body<PartData.FileItem>()
            }
            HttpStatusCode.BadRequest to {
                description = "Invalid parameters"
            }
        }
    }

    val getHarbourLocation: RouteConfig.() -> Unit = {
        tags("harbourMaster")
        description = "Get the assigned location for the authenticated harbor master"
        request {
            queryParameter<String>("timezone") {
                description =
                    "Optional timezone ('Europe/Berlin'). Defaults to Ip address based timezone. Backup UTC."
                required = false
            }
        }
        response {
            HttpStatusCode.OK to {
                description = "Successful response with location details"
                body<DetailedLocationDTO>()
            }
            HttpStatusCode.Unauthorized to {
                description = "User is not a harbor master"
            }
            HttpStatusCode.NotFound to {
                description = "No location assigned to this harbor master"
            }
        }
    }

    val putLocation: RouteConfig.() -> Unit = {
        description = "Update the location information"
        tags("location")
        request {
            pathParameter<Long>("id") {
                description = "The location ID (not the sensor ID)"
            }
            queryParameter<String>("timezone") {
                description =
                    "Optional timezone ('Europe/Berlin'). Defaults to Ip address based timezone. Backup UTC."
                required = false
            }
            body<UpdateLocationRequest> {
                example("Update Location Example") {
                    value = UpdateLocationRequest(
                        name = "Updated Marina Test",
                        description = "This is a new description for the marina.",
                        address = "456 Pier Road",
                        openingHours = "Mon-Fri: 09:00-17:00; Sat: 10:00-14:00",
                        contact = Contact(
                            phone = "1234567890",
                            email = "info@marina.com",
                            website = "https://marina-updated.com"
                        ),
                        image = ImageRequest(
                            base64 = "/9j/4AAQSkZJRgABAQAAAQABAAD/2wBDAAIB...",
                            contentType = "image/jpeg"
                        ),
                    )
                }
            }
        }
        response {
            HttpStatusCode.OK to {
                body<DetailedLocationDTO>()
            }
            HttpStatusCode.NotFound to {
                body<String>()
            }
        }
    }

    val deleteLocationImage: RouteConfig.() -> Unit = {
        tags("location")
        description = "Delete a locations image"
        request {
            pathParameter<Long>("id") {
                description = "The location ID (not the sensor ID)"
            }
        }
        response {
            HttpStatusCode.OK to {
                body<Unit>()
            }
            HttpStatusCode.BadRequest to {
                description = "Invalid parameters"
            }
        }
    }

}