package datasource.notificaiton

interface NotificationFactory {
    fun createSender(type: NotificationType): NotificationSender
}

