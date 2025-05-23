# Kotlin Coroutine Utility

A powerful utility library for handling concurrent and sequential coroutine operations in Kotlin with built-in error handling, retry mechanisms, and parallel execution control.

## Features

### Concurrent Operations
- **Parallel Job Execution**: Run multiple coroutine jobs concurrently with controlled parallelism
- **Result Collection**: Execute concurrent jobs and collect their results
- **Grouped Jobs**: Run jobs in groups with result and error collection
- **Fail-Fast Behavior**: Optional fail-fast behavior for concurrent operations
- **Supervisor Job Support**: Built-in support for supervisor jobs to prevent cascading failures

### Sequential Operations
- **Sequential Job Execution**: Run jobs in sequence with result passing between steps
- **Error Handling**: Comprehensive error handling for sequential operations
- **Result Propagation**: Pass results from one job to the next in the sequence

### Common Features
- **Retry Mechanism**: Configurable retry logic with initial delay
- **Timeout Support**: Set timeouts for individual operations
- **Error Handling**: Customizable error handling for all operations
- **Parallelism Control**: Control the number of concurrent operations
- **Resource Management**: Automatic resource cleanup and cancellation

## Usage Examples

### Running Concurrent Jobs
```kotlin
val configs = listOf<SafeJobConfig<Unit>>(
    SafeJobConfig(
        block = { /* Your job logic */ },
        timeoutMillis = 5000,
        maxRetries = 3
    ),
    SafeJobConfig(
        block = { /* Your job logic */ },
        timeoutMillis = 10000,
        maxRetries = 3
    )
)

// Run concurrent jobs
val jobs = runConcurrentJobs(
    configs = configs,
    parallelism = 2,
    errorHandler = { println("Error: ${it.message}") }
)

// Wait for all jobs to complete
jobs.awaitAll()
```

### Running Sequential Jobs
```kotlin
val configs = listOf(
    SafeJobConfigSequential<Any?, String>(
        block = { 
            "input to next block"
        },
        timeoutMillis = 5000,
        maxRetries = 3
    ),
    SafeJobConfigSequential<String, String>(
        block = { inputFromPreviousBlock ->
            println(inputFromPreviousBlock)
        }
    )
)

// Run sequential jobs
val result = runSequentialJobs<String>(
    configs = configs,
    errorHandler = { println("Error: ${it.message}") }
)

// Handle result
println(result.getOrElse { "Failed: ${it.message}" })
```

### Running Grouped Jobs
```kotlin
val configs = listOf<SafeJobConfig<String>>(
    SafeJobConfig(
        block = { 
            "executed block 1"
        },
        maxRetries = 3
    ),
    SafeJobConfig(
        block = {
             "executed block 2"
        }
    )
)


val (results, errors) = runGroupedJobs(
    configs = configs,
    parallelism = 2,
    errorHandler = { println("Error: ${it.message}") }
)

// Process results and errors
println("Successful results: $results")
println("Errors: $errors")
```

## Configuration Options

### SafeJobConfig
- `block`: The coroutine block to execute
- `timeoutMillis`: Optional timeout in milliseconds
- `maxRetries`: Maximum number of retry attempts
- `initialDelayMillis`: Initial delay before first retry

### SafeJobConfigSequential
- `block`: The coroutine block to execute
- `timeoutMillis`: Optional timeout in milliseconds
- `maxRetries`: Maximum number of retry attempts
- `initialDelayMillis`: Initial delay before first retry

## Error Handling

The library provides comprehensive error handling capabilities:
- Custom error handlers for all operations
- Automatic retry mechanism
- Error collection in grouped operations
- Result type for sequential operations
- Cancellation handling

## Requirements

- Kotlin 1.3.0 or higher
- Kotlinx Coroutines library

## License

This project is open source and available under the MIT License. 
