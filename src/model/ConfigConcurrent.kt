package model

import CoroutineUtils.SafeJobConfig
import datasource.notificaiton.NotificationFactoryTestImpl
import datasource.notificaiton.NotificationType

object ConfigConcurrent {
    fun getConcurrentNotificationJobs() = listOf<SafeJobConfig<Unit>>(
        SafeJobConfig(
            block = {
                NotificationFactoryTestImpl
                    .createSender(NotificationType.EMAIL)
                    .send("faisal","Hello! Email")
            }
        ),
        SafeJobConfig(
            block = {
                NotificationFactoryTestImpl
                    .createSender(NotificationType.SMS)
                    .send("faisal","Hello! SMS")
            }
        ),
        SafeJobConfig(
            block = {

                NotificationFactoryTestImpl
                    .createSender(NotificationType.UNKNOWN_EXCEPTION)
                    .send("faisal","Hello! SMS")
            },
            maxRetries = 1
        ),
        SafeJobConfig(
            block = {
                NotificationFactoryTestImpl
                    .createSender(NotificationType.PUSH)
                    .send("faisal","Hello! Push")
            }
        )
    )
}