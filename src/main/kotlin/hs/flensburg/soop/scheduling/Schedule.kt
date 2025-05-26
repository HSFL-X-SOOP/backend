package hs.flensburg.soop.scheduling

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.application.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration


private val logger = KotlinLogging.logger("Scheduling")

fun Application.configureScheduling() = GlobalScope.launch(Dispatchers.IO) {

}

fun CoroutineScope.scheduleTask(
    delayMillis: Duration,
    runAtStart: Boolean = false,
    task: suspend CoroutineScope.() -> Unit
): Job = launch {
    if (runAtStart) {
        try {
            task()
        } catch (e: Exception) {
            logger.error(e) { "Error while executing a scheduled task" }
        }
    }

    while (true) {
        try {
            delay(delayMillis)
            task()
        } catch (e: Exception) {
            logger.error(e) { "Error while executing a scheduled task" }
        }
    }
}