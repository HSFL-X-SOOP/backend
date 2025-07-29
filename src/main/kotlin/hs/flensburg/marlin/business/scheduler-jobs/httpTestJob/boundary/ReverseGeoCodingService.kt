package hs.flensburg.marlin.business.`scheduler-jobs`.httpTestJob.boundary

import de.lambda9.tailwind.jooq.Jooq
import hs.flensburg.marlin.business.App
import hs.flensburg.marlin.business.httpclient
import hs.flensburg.marlin.business.`scheduler-jobs`.httpTestJob.entity.NominatimResponse
import hs.flensburg.marlin.database.generated.tables.references.LOCATION
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.runBlocking
import org.jooq.exception.DataAccessException
import kotlin.time.Duration.Companion.seconds

object ReverseGeoCodingService {

    fun updateLocationNames(): App<DataAccessException, Unit> = Jooq.query {
        println("Starte Geocoding")
        // get all locations without a name
        val locationsWithoutNames = """
            SELECT id, ST_X(coordinates::geometry) as lon, ST_Y(coordinates::geometry) as lat
            FROM marlin.location 
            WHERE name IS NULL 
            AND coordinates IS NOT NULL
        """.trimIndent()

        val locations = resultQuery(locationsWithoutNames)
            .fetch()
            .map { record ->
                Triple(
                    record.get("id", Long::class.java),
                    record.get("lon", Double::class.java),
                    record.get("lat", Double::class.java)
                )
            }

        // update each location
        locations.forEach { (id, lon, lat) ->
            try {
                // use delay (Nominatim Usage Policy)
                Thread.sleep(1.seconds.inWholeMilliseconds)

                // fetch the location name from Nominatim
                runBlocking {
                    val response = httpclient.get("https://nominatim.openstreetmap.org/reverse") {
                        parameter("lat", lat)
                        parameter("lon", lon)
                        parameter("format", "json")
                        header("User-Agent", "SOOP-Application/1.0")
                    }.body<NominatimResponse>()

                    println(response)

                    var shortName = "Unknown Location"

                    if (response.display_name != null) {
                        // Extract only the name
                        shortName = response.display_name.split(",")[0].trim()
                    }
                    // Update the location name in the database
                    update(LOCATION)
                        .set(LOCATION.NAME, shortName)
                        .where(LOCATION.ID.eq(id))
                        .execute()
                }
            } catch (e: Exception) {
                // report the error
                println("Fehler beim Geocoding für Location $id: ${e.message}")
            }
        }
    }
}
