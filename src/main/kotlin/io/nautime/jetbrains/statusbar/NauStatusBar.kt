package io.nautime.jetbrains.statusbar

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.ListPopup
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.IconLoader.getIcon
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory
import com.intellij.openapi.wm.WindowManager
import com.intellij.util.Consumer
import com.intellij.util.ui.UIUtil
import io.nautime.jetbrains.NauPlugin
import org.jetbrains.annotations.NonNls
import java.awt.event.MouseEvent
import javax.swing.Icon

class NauStatusBar : StatusBarWidgetFactory {

    override fun getId(): String = WIDGET_ID

    override fun getDisplayName(): String = "Nau"

    override fun createWidget(project: Project): StatusBarWidget = NauStatusBarWidget(project)

    override fun canBeEnabledOn(statusBar: StatusBar): Boolean = true

    override fun disposeWidget(widget: StatusBarWidget) = Disposer.dispose(widget)

    override fun isAvailable(project: Project): Boolean = true

//    override fun disposeWidget(widget: StatusBarWidget) {}

    inner class NauStatusBarWidget constructor(val project: Project) : StatusBarWidget {
        val statusBar: StatusBar? = WindowManager.getInstance().getStatusBar(project)

        override fun ID(): String = WIDGET_ID

        override fun getPresentation(): StatusBarWidget.WidgetPresentation {
            return StatusBarImpl(this)
        }

        override fun install(statusBar: StatusBar) {}
        override fun dispose() {}

        private inner class StatusBarImpl(private val widget: NauStatusBarWidget) : StatusBarWidget.MultipleTextValuesPresentation, StatusBarWidget.Multiframe {
//            private val panel: TextPanel = TextPanel.WithIconAndArrows()

            override fun getPopup(): JBPopup? {
                if (NauPlugin.getState().isLinked) {
                    BrowserUtil.browse(NauPlugin.getDashboardUrl())
                } else {
                    BrowserUtil.browse(NauPlugin.getPluginLinkUrl())
                }
                if (widget.statusBar != null) widget.statusBar.updateWidget(WIDGET_ID)
                return null
            }

            @Deprecated("implement {@link #getPopup()}")
            override fun getPopupStep(): ListPopup? = null

            override fun getSelectedValue(): String = NauPlugin.getStatusBarText()

            override fun getIcon(): Icon {
                val theme = if (UIUtil.isUnderDarcula()) "dark" else "light"
                return getIcon("/icons/status-bar-icon-$theme.svg", NauPlugin::class.java)
            }

            override fun getTooltipText(): String? = null

            override fun getClickConsumer(): Consumer<MouseEvent>? = null

            override fun copy(): StatusBarWidget {
                return NauStatusBarWidget(widget.project)
            }

            override fun ID(): @NonNls String = WIDGET_ID

            override fun install(statusBar: StatusBar) {}
            override fun dispose() {
                Disposer.dispose(widget)
            }
        }
    }

    companion object {
        const val WIDGET_ID = "Nau"
    }
}
