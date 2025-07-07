package hs.flensburg.soop.business

import de.lambda9.tailwind.core.Exit
import de.lambda9.tailwind.core.KIO
import de.lambda9.tailwind.core.KIO.Companion.unsafeRunSync
import de.lambda9.tailwind.core.extensions.exit.fold
import hs.flensburg.soop.business.jobs.httpTestJob.boundary.SensorDataService
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes


private val logger = KotlinLogging.logger("Scheduling")

@OptIn(DelicateCoroutinesApi::class)
fun configureScheduling(env: JEnv) = GlobalScope.launch(Dispatchers.IO) {


    schedule(1.minutes, true) {
        SensorDataService.getMultipleSensorData(env = env)
    }

}

/**
 * Schedules a task that executes a function periodically.
 *
 * @param delayMillis The delay between each execution of the task.
 * @param runAtStart If true, the task will be executed immediately before the first delay.
 * @param task The suspend function that represents the task to be executed.
 * @return A Job representing the scheduled task.
 */
fun CoroutineScope.schedule(
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

/**
 * Schedules a task that returns a KIO instance to be executed periodically.
 *
 * @param delayMillis The delay between each execution of the task.
 * @param runAtStart If true, the task will be executed immediately before the first delay.
 * @param env The environment containing the Jooq instance.
 * @param block The suspend function that represents the task to be executed.
 */
fun <E, A> CoroutineScope.schedule(
    delayMillis: Duration,
    runAtStart: Boolean = false,
    env: JEnv,
    block: suspend CoroutineScope.() -> KIO<JEnv, E, A>,
): Job = launch {
    var result: A? = null

    if (runAtStart) {
        val exit = block().unsafeRunSync(env)
        result = exit.get()
    }

    while (true) {
        delay(delayMillis)
        val exit = block().unsafeRunSync(env)
        result = exit.get()
    }
}

private fun <E, A> Exit<E, A>.get(): A? {
    return this.fold(
        onDefect = {
            logger.error(it) { "Error while executing a scheduled task" }
            null
        },
        onError = {
            logger.error { "Error while executing a scheduled task: ${it.toString()}" }
            null
        },
        onSuccess = { it }
    )
}
