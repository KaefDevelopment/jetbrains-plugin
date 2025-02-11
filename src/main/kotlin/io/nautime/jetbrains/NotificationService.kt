package io.nautime.jetbrains

import com.intellij.notification.BrowseNotificationAction
import com.intellij.notification.Notification
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages
import io.nautime.jetbrains.model.Notif


class NotificationService(
    private val nauPlugin: NauPlugin,
) {

//     todo extend BrowseNotificationAction and track clicks

    fun showGoToLinkNotif() {
        val notification: Notification = GROUP.createNotification(
            "Welcome to NauTime Tracker",
            "Follow the link below to start using the plugin.",
            NotificationType.INFORMATION
        )
            .setImportant(true)
//            .setIcon(KeyPromoterIcons.KP_ICON)
            .addAction(
                object : BrowseNotificationAction(
                    "Link plugin",
                    nauPlugin.getPluginLinkUrl()
                ) {
                    override fun actionPerformed(e: AnActionEvent, notification: Notification) {
                        super.actionPerformed(e, notification)
                        nauPlugin.checkLink()
                    }
                }
            )
        notification.notify(null)


    }

    fun showLinkIsDoneNotif() {
        val notification: Notification = GROUP.createNotification(
            "Congrats!",
            "You are linked to NauTime",
            NotificationType.INFORMATION
        )
            .setImportant(true)
            .addAction(
                BrowseNotificationAction(
                    "Browse your stats",
                    nauPlugin.getDashboardUrl()
                )
            )
//            .setIcon(KeyPromoterIcons.KP_ICON)
        notification.notify(null)
    }

    fun show(notif: Notif) {
        val notification: Notification = GROUP.createNotification(
            notif.title,
            notif.message,
            notif.type.ideType
        )
            .setImportant(notif.important)
            .addAction(
                BrowseNotificationAction(
                    notif.linkText,
                    notif.link
                )
            )
//            .setIcon(KeyPromoterIcons.KP_ICON)
        notification.notify(null)
    }

    fun showWarningNotif(title: String, message: String) {
        Messages.showWarningDialog(message, title)
    }


    companion object {
        private val GROUP = NotificationGroupManager.getInstance().getNotificationGroup(NauPlugin.PLUGIN_ID)
    }


}
