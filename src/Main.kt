
import datasource.notificaiton.NotificationFactoryTestImpl
import datasource.notificaiton.NotificationType
import datasource.weather.WeatherServiceImpl
import kotlinx.coroutines.*
import CoroutineUtils.SafeJobConfig
import CoroutineUtils.SafeJobConfigSequential
import CoroutineUtils.runConcurrentJobs
import CoroutineUtils.runConcurrentJobsWithResults
import CoroutineUtils.runSequentialJobs
import model.City
import model.ConfigConcurrent.getConcurrentNotificationJobs
import model.ConfigSequential
import model.ConfigSequential.getNotificationsSequentialJobs
import model.Weather


fun main() = runBlocking {
    //***Parallel tests***

    //runDeferredGroupTest()
   // runSafeJobTest()
    //runConcurrentJobsWithResultTest()

    //***Sequential tests***

    //runSequentialJobsDeferredTest()
    runWeatherSequentialJob()
}


//parallel

suspend fun runDeferredGroupTest(){
    val configs: List<SafeJobConfig<String>> = listOf(
        SafeJobConfig(
            block = {
                NotificationFactoryTestImpl
                    .createSender(NotificationType.EMAIL)
                    .send("faisal","Hello! Email")
                "Email sent"
            }
        ),
        SafeJobConfig(
            block = {
                NotificationFactoryTestImpl
                    .createSender(NotificationType.SMS)
                    .send("faisal","Hello! SMS")
                "SMS sent"
            }
        ),
        SafeJobConfig(
            block = {
                NotificationFactoryTestImpl
                    .createSender(NotificationType.UNKNOWN_EXCEPTION)
                    .send("faisal","Hello! EXCEPTION")
                "EXCEPTION Handled"
            },
            maxRetries = 0
        ),
        SafeJobConfig(
            block = {
                    NotificationFactoryTestImpl
                        .createSender(NotificationType.PUSH)
                        .send("faisal","Hello! Push")
                "Push sent"
            }
        )
    )

    println("Starting grouped jobs...\n")

    try {
        val failFastScope = CoroutineScope(Job() + Dispatchers.Default)
        val (results, errors) = CoroutineUtils.runGroupedJobs(
            configs = configs,
            parallelism = 2,
            scope = failFastScope,
            errorHandler = { println("Caught error: ${it.message}") }
        )

        println("\n✅ Successful results:")
        results.forEach { println(it) }

        println("\n❌ Errors:")
        errors.forEach { println(it.message) }
    } catch (e: Throwable) {
        println("Job failed with error: ${e.message}")
    }

}
suspend fun runSafeJobTest(){

    println("Starting safe jobs...\n")

    // 2) Fail-fast behavior: pass your own scope with plain Job()
    //comment this fastFailScope so that default scope is Supervisor Scope
    val failFastScope = CoroutineScope(Job() + Dispatchers.Default)

    try {
        val jobs = runConcurrentJobs(
            configs = getConcurrentNotificationJobs(),
            scope = failFastScope,
            errorHandler = { println("FailFast caught error: ${it.message}") },
            parallelism = 2
        )

        jobs.awaitAll() // now this will throw on first failure (if not using SupervisorJob)
        println("All jobs completed successfully")

    } catch (e: Throwable) {
        println("RunSafeJobs failed fast with exception: ${e.message}")
    }

    println("All jobs completed (fail-fast with passed scope)")
}


suspend fun runConcurrentJobsWithResultTest(){

    val failFastScope = CoroutineScope(Job() + Dispatchers.Default)
    try {
        val jobs = runConcurrentJobsWithResults(
            configs = getConcurrentNotificationJobs() ,
//            scope = failFastScope ,
            errorHandler = { println("FailFast caught error: ${it.message}") },
            parallelism = 2
        )

        jobs.joinAll() // will throw on first failure and cancel siblings
    } catch (e: Throwable) {
        println("RunSafeJobs failed fast with exception: ${e.message}")
    }


}


//sequentials
suspend fun runWeatherSequentialJob() {

    val result = runSequentialJobs<String>(
        configs = ConfigSequential.getWeatherSequentialJobs(),
        errorHandler = { println("Error occurred: ${it.message}") }
    )

    println("Final Result:")
    println(result.getOrElse { "Failed: ${it.message}" })
}
suspend fun runSequentialJobsDeferredTest(){

    val resultDeferred = runSequentialJobs<String>(
        configs = getNotificationsSequentialJobs(),
        scope = CoroutineScope(Dispatchers.Default),
        errorHandler = { println("Caught: ${it.message}") }
    )
    println("Result: $resultDeferred")


}




