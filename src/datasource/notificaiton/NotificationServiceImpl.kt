package datasource.notificaiton
import kotlinx.coroutines.delay

enum class NotificationType {
    EMAIL, SMS, PUSH, UNKNOWN_EXCEPTION
}
interface NotificationSender {
    suspend fun send(to: String, message: String)
}
class EmailTestSender : NotificationSender {
    override suspend fun send(to: String, message: String) {
        delay(200)
        println("Email sent to $to: $message")
    }
}

class SmsTestSender : NotificationSender {
    override suspend fun send(to: String, message: String) {
        delay(500)
        println("SMS sent to $to: $message")
    }
}

class PushTestSender : NotificationSender {
    override suspend fun send(to: String, message: String) {
        delay(100)
        println("Push notification sent to $to: $message")
    }
}

class UnknownTestSender : NotificationSender {
    override suspend fun send(to: String, message: String) {
        println("starting method with exception")
        delay(50)
        throw IllegalStateException("unknown task failed")
    }
}

object NotificationFactoryTestImpl : NotificationFactory {
   override fun createSender(type: NotificationType): NotificationSender {
        return when (type) {
            NotificationType.EMAIL -> EmailTestSender()
            NotificationType.SMS -> SmsTestSender()
            NotificationType.PUSH -> PushTestSender()
            NotificationType.UNKNOWN_EXCEPTION -> UnknownTestSender()
        }
    }
}
