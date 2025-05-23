package model

import CoroutineUtils.SafeJobConfigSequential
import datasource.notificaiton.NotificationFactoryTestImpl
import datasource.notificaiton.NotificationType
import datasource.weather.WeatherServiceImpl

object ConfigSequential {
    fun getWeatherSequentialJobs() = listOf(
        SafeJobConfigSequential<Any?, List<City>>(
            block = {
                println("Fetching cities...")
                WeatherServiceImpl.fetchCities()
            }
        ),
        SafeJobConfigSequential<List<City>, List<Weather>>(
            block = { cities ->
                println("Fetching weather for cities...")
                WeatherServiceImpl.fetchWeatherForCities(cities ?: emptyList())
            }
        ),
        SafeJobConfigSequential<List<Weather>, String>(
            block = { weathers ->
                println("Summarizing weather report...")
                weathers?.joinToString(separator = "\n") { "${it.city}: ${it.forecast}" } ?: "No data"
            }
        )
    )

    fun getNotificationsSequentialJobs() = listOf(
        SafeJobConfigSequential<Any?, String>(
            block = {
                NotificationFactoryTestImpl
                    .createSender(NotificationType.EMAIL)
                    .send("faisal", "Hello! Email")
                "Email sent"
            }
        ),
        SafeJobConfigSequential<String, String>(
            block = { inputFromPreviousBlock ->
                NotificationFactoryTestImpl
                    .createSender(NotificationType.SMS)
                    .send("faisal", "Hello! SMS")
                "${inputFromPreviousBlock}, SMS sent"
            }
        ),
//        SafeJobConfigSequential<String, String>(
//            block = { inputFromPreviousBlock ->
//                NotificationFactoryTestImpl
//                    .createSender(NotificationType.UNKNOWN_EXCEPTION)
//                    .send("faisal", "Hello! Unknown")
//                "${inputFromPreviousBlock}, UNKNOWN_EXCEPTION sent"
//            }
//        ),
        SafeJobConfigSequential<String, String>(
            block = { inputFromPreviousBlock ->
                NotificationFactoryTestImpl
                    .createSender(NotificationType.PUSH)
                    .send("faisal", "Hello! Push")
                "${inputFromPreviousBlock}, Push sent"
            }
        )
    )
}