
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.SupervisorJob

object CoroutineUtils {

    data class SafeJobConfig<T>(
        val block: suspend CoroutineScope.(T?) -> T,
        val timeoutMillis: Long? = null,
        val maxRetries: Int = 0,
        val initialDelayMillis: Long = 0
    )


    data class SafeJobConfigSequential<Tin,Tout>(
        val block: suspend CoroutineScope.(Tin?) -> Tout,
        val timeoutMillis: Long? = null,
        val maxRetries: Int = 0,
        val initialDelayMillis: Long = 0
    )


    fun runConcurrentJobs(
        configs: List<SafeJobConfig<Unit>>,
        scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
        context: CoroutineContext = Dispatchers.Default,
        errorHandler: (Throwable) -> Unit = { it.printStackTrace() },
        parallelism: Int = configs.size
    ): List<Deferred<Unit?>> {

        val semaphore = Semaphore(parallelism)

        return configs.map { config ->
            scope.async(context) {
                semaphore.withPermit {
                    try {
                        executeWithRetry(
                            config.timeoutMillis,
                            config.maxRetries,
                            config.initialDelayMillis,
                            { e ->
                                if (e !is CancellationException) {
                                    errorHandler(e)
                                }
                            }
                        ) {
                            with(config) { block(null) }
                        }
                    } catch (e: Throwable) {
                        if (e !is CancellationException) {
                            errorHandler(e)
                            // Rethrow if not using SupervisorJob for fail-fast behavior
                            if (!scope.isUsingSupervisorJob()) throw e
                        }
                        null
                    }
                }
            }
        }
    }




    fun <T> runConcurrentJobsWithResults(
        configs: List<SafeJobConfig<T>>,
        scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
        context: CoroutineContext = Dispatchers.Default,
        errorHandler: (Throwable) -> Unit = { it.printStackTrace() },
        parallelism: Int = configs.size
    ): List<Deferred<T?>> {
        val semaphore = Semaphore(parallelism)
        return configs.map { config ->
            scope.async(context) {
                try {
                    semaphore.withPermit {
                        executeWithRetry(
                            config.timeoutMillis,
                            config.maxRetries,
                            config.initialDelayMillis,
                            errorHandler
                        ) {
                            with(config) { block(null) }
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Throwable) {
                    errorHandler(e)
                    throw e
                }
            }
        }
    }

    suspend fun <T> runGroupedJobs(
        configs: List<SafeJobConfig<T>>,
        scope: CoroutineScope =  CoroutineScope(SupervisorJob() + Dispatchers.Default),
        context: CoroutineContext = Dispatchers.Default,
        parallelism: Int? = null,
        errorHandler: (Throwable) -> Unit = { it.printStackTrace() }
    ): Pair<List<T>, List<Throwable>> {

        val results = mutableListOf<T>()
        val errors = mutableListOf<Throwable>()
        val resultMutex = Mutex()
        val errorMutex = Mutex()
        val semaphore = parallelism?.let { Semaphore(it) }





        val jobs = configs.map { config ->
            scope.async(context) {
                semaphore?.acquire()
                try {
                    val result = executeWithRetry(
                        config.timeoutMillis,
                        config.maxRetries,
                        config.initialDelayMillis,
                        errorHandler
                    ) {
                        with(config) { block(null) }
                    }
                    resultMutex.withLock { results.add(result) }
                } catch (e: Throwable) {
                    errorHandler(e)
                    errorMutex.withLock { errors.add(e) }
                   if (!scope.isUsingSupervisorJob()) throw e
                } finally {
                    semaphore?.release()
                }
            }
        }

        try {
            jobs.awaitAll() // Fails fast if not Supervisor
        } catch (_: Throwable) {
            // Swallow, errors already collected
        }

        return Pair(results, errors)
    }





    suspend fun <T> runSequentialJobs(
        configs: List<SafeJobConfigSequential<*, *>>,
        scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
        errorHandler: (Throwable) -> Unit = { it.printStackTrace() }
    ): Result<T> = scope.async {
        try {
            var previousResult: Any? = null
            for (config in configs) {
                @Suppress("UNCHECKED_CAST")
                val typedConfig = config as SafeJobConfigSequential<Any?, Any?>
                previousResult = executeWithRetry(
                    timeoutMillis = typedConfig.timeoutMillis,
                    maxRetries = typedConfig.maxRetries,
                    initialDelayMillis = typedConfig.initialDelayMillis,
                    errorHandler = errorHandler
                ) {
                    with(typedConfig) { block(previousResult) }
                }
            }
            @Suppress("UNCHECKED_CAST")
            Result.success(previousResult as T)
        } catch (e: Throwable) {
            errorHandler(e)
            Result.failure(e)
        }
    }.await()



    suspend fun <T> executeWithRetry(
        timeoutMillis: Long? = null,
        maxRetries: Int = 0,
        initialDelayMillis: Long = 0,
        errorHandler: (Throwable) -> Unit = {},
        block: suspend () -> T
    ): T {
        var currentAttempt = 0
        if (initialDelayMillis > 0) delay(initialDelayMillis)

        while (true) {
            try {
                return if (timeoutMillis != null) {
                    withTimeout(timeoutMillis) { block() }
                } else {
                    block()
                }
            } catch (e: Throwable) {

                    errorHandler(e)

                if (++currentAttempt > maxRetries) throw e
            }
        }
    }
}


fun CoroutineScope.isUsingSupervisorJob(): Boolean {
    return coroutineContext[Job]?.let { it::class.simpleName?.contains("Supervisor") == true } == true
}
