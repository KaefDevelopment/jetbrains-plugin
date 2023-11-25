package io.nautime.jetbrains.model

import com.intellij.notification.NotificationType
import io.nautime.jetbrains.NauPlugin
import kotlinx.serialization.Serializable

@Serializable
data class PluginStatusResponse(
    val auth: Boolean,
    val cliVersion: String,
    val stats: Stats,
    val notificationList: List<Notif>,
) {
    companion object {
        val DEFAULT = PluginStatusResponse(
            auth = NauPlugin.getState().isLinked,
            cliVersion = NauPlugin.getState().currentCliVer,
            stats = Stats(0),
            notificationList = emptyList()
        )
    }

}

@Serializable
data class Stats(
    val total: Long,
)

@Serializable
class Notif(
    val title: String,
    val message: String,
    val linkText: String,
    val link: String,
    val type: NotifType = NotifType.INFO,
    val important: Boolean = true,
)


enum class NotifType(val ideType: NotificationType) {
    INFO(NotificationType.INFORMATION),
    WARN(NotificationType.WARNING),
}
