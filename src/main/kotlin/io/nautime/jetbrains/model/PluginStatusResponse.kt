package io.nautime.jetbrains.model

import com.intellij.notification.NotificationType
import io.nautime.jetbrains.NauPlugin
import kotlinx.serialization.Serializable

@Serializable
data class PluginStatusResponse(
    val auth: Boolean,
    val cliVersion: String,
    val stats: Stats,
    val tooltip: TooltipData,
    val notificationList: List<Notif>,
) {
    companion object {
        fun default(nauPlugin: NauPlugin) = PluginStatusResponse(
            auth = nauPlugin.getState().isLinked,
            cliVersion = nauPlugin.getState().currentCliVer,
            stats = Stats(0, null),
            tooltip = TooltipData("Nau"),
            notificationList = emptyList()
        )
    }

    override fun toString(): String {
        return "PluginStatusResponse(auth=$auth, cliVersion='$cliVersion', stats=$stats, tooltip=$tooltip, notificationList=${notificationList.size})"
    }
}

@Serializable
data class Stats(
    val total: Long,
    val goal: GoalStats?,
)

@Serializable
data class TooltipData(
    val html: String,
) {
    override fun toString(): String {
        return "TooltipData(html='length:${html.length}')"
    }
}

@Serializable
data class GoalStats(
    val duration: Long,
    val percent: Long,
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
