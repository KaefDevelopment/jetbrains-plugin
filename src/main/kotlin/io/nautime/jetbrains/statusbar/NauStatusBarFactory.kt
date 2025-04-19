package io.nautime.jetbrains.statusbar

import com.intellij.ide.BrowserUtil
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.Separator
import com.intellij.openapi.actionSystem.ex.CheckboxAction
import com.intellij.openapi.components.service
import com.intellij.openapi.components.serviceOrNull
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.ListPopup
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.IconLoader.getIcon
import com.intellij.openapi.wm.CustomStatusBarWidget
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidget.WidgetPresentation
import com.intellij.openapi.wm.StatusBarWidgetFactory
import com.intellij.openapi.wm.impl.status.TextPanel
import com.intellij.ui.ClickListener
import com.intellij.ui.Gray
import com.intellij.ui.JBColor
import com.intellij.ui.awt.RelativePoint
import com.intellij.util.LazyInitializer
import com.intellij.util.concurrency.EdtExecutorService
import com.intellij.util.ui.update.Activatable
import com.intellij.util.ui.update.UiNotifyConnector
import io.nautime.jetbrains.NauPlugin
import java.awt.Color
import java.awt.Graphics
import java.awt.Point
import java.awt.event.MouseEvent
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import javax.swing.JComponent

class NauStatusBarFactory : StatusBarWidgetFactory {

    override fun getId(): String = WIDGET_ID

    override fun getDisplayName(): String = "Nau"

    override fun createWidget(project: Project): StatusBarWidget = NauStatusBarPanel(project)

    override fun canBeEnabledOn(statusBar: StatusBar): Boolean = true

    override fun disposeWidget(widget: StatusBarWidget) = Disposer.dispose(widget)

    override fun isAvailable(project: Project): Boolean = true

    companion object {
        const val WIDGET_ID = "Nau"
    }
}


class NauStatusBarPanel(val project: Project) : CustomStatusBarWidget, Activatable {
    private val widget = LazyInitializer.create { NauStatsPanel(this) }

    private var updateFuture: ScheduledFuture<*>? = null

    override fun getComponent(): JComponent = widget.get()

    override fun ID(): String = NauStatusBarFactory.WIDGET_ID

    override fun showNotify() {
        updateFuture = EdtExecutorService.getScheduledExecutorInstance().scheduleWithFixedDelay(
            { widget.get().updateState() }, 0, 10, TimeUnit.SECONDS
        )
    }

    override fun hideNotify() {
        updateFuture?.cancel(true)
        updateFuture = null
    }

    override fun dispose() {
        updateFuture?.cancel(true)
        updateFuture = null
    }

    override fun getPresentation(): WidgetPresentation? {
        return null
    }

    override fun install(statusBar: StatusBar) {}
}

private class NauStatsPanel(private val widget: NauStatusBarPanel) : TextPanel.WithIconAndArrows() {
    val nauPlugin = service<NauPlugin>()

    init {
        object : ClickListener() {
            override fun onClick(event: MouseEvent, clickCount: Int): Boolean {
                val nauPlugin = serviceOrNull<NauPlugin>() ?: return true

                showPopup(nauPlugin, event)
                return true
            }
        }.installOn(this, true)

        this.isFocusable = false
        this.setTextAlignment(0f)
        this.updateUI()
        UiNotifyConnector(this, widget)
    }

    private fun showPopup(nauPlugin: NauPlugin, e: MouseEvent) {
//        if(!nauPlugin.getState().isLinked) {
//            BrowserUtil.browse(nauPlugin.getPluginLinkUrl())
//            return
//        }

        val context = DataManager.getInstance().getDataContext(this)

        val popup = createPopup(nauPlugin, context)
        val dimension = popup.content.preferredSize
        val at = Point(-dimension.width / 2, -dimension.height)
        popup.show(RelativePoint(e.component, at))

        Disposer.tryRegister(widget, popup) // destroy popup on unexpected project close
    }

    private fun createPopup(nauPlugin: NauPlugin, context: DataContext): ListPopup {
        val popupGroup = DefaultActionGroup()

        if (nauPlugin.getState().isLinked) {
            popupGroup.add(NauAction("Open dashboard", this) {
                BrowserUtil.browse(nauPlugin.getDashboardUrl())
            })
        } else {
            popupGroup.add(NauAction("Link plugin", this) {
                BrowserUtil.browse(nauPlugin.getPluginLinkUrl())
                nauPlugin.checkLink()
            })
        }

        popupGroup.add(Separator.getInstance())

        popupGroup.add(NauCheckbox(
            text = "Show stats",
            panel = this,
            isSelectedFunc = { nauPlugin.getState().showStats },
            setSelectedFunc = { state -> nauPlugin.getState().showStats = state }
        ))

        popupGroup.add(NauCheckbox(
            text = "Show goal progress",
            panel = this,
            isSelectedFunc = { nauPlugin.getState().showGoalProgress },
            setSelectedFunc = { state -> nauPlugin.getState().showGoalProgress = state }
        ))

        return JBPopupFactory.getInstance().createActionGroupPopup(
            "Nau Time Tracker",
            popupGroup, context, JBPopupFactory.ActionSelectionAid.SPEEDSEARCH, false
        )
    }


    class NauAction(
        text: String,
        val panel: NauStatsPanel,
        val action: () -> Unit,
    ) : DumbAwareAction(text) {
        override fun actionPerformed(e: AnActionEvent) {
            action()
            panel.updateState()
        }
    }

    class NauCheckbox(
        text: String,
        val panel: NauStatsPanel,
        val isSelectedFunc: () -> Boolean,
        val setSelectedFunc: (Boolean) -> Unit,
    ) : CheckboxAction(text) {
        override fun isSelected(e: AnActionEvent): Boolean {
            return isSelectedFunc()
        }

        override fun setSelected(e: AnActionEvent, state: Boolean) {
            setSelectedFunc(state)
            panel.updateState()
        }

        override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
    }

    val goalBackgroundColor = JBColor.namedColor("Nau.GoalBackgroundColor", JBColor(Gray._215, Gray._90))
    val goalColor = JBColor.namedColor("Nau.goalColor", JBColor(Color(0, 128, 0), Color(0, 128, 0)))

    public override fun paintComponent(g: Graphics) {
        val goalStats = nauPlugin.getStats()?.goal
        if (nauPlugin.getState().showGoalProgress && goalStats != null) {
            g.color = goalBackgroundColor
            g.fillRect(5, 0, size.width - 5, 1)

            g.color = goalColor
            g.fillRect(5, 0, ((size.width - 5) * goalStats.percent / 100).toInt(), 1)
        }
        super.paintComponent(g)
    }

    private val statusBarText: String
        get() = nauPlugin.getStatusBarText()

    override val textForPreferredSize: String
        get() = statusBarText


    fun updateState() {
        if (!this.isShowing) return

        text = statusBarText
        toolTipText = nauPlugin.getStatusBarTooltipText()
        icon = getIcon(ICON_PATH, NauStatsPanel::class.java.classLoader)
    }


    companion object {
        const val ICON_PATH = "/icons/status-bar-icon.svg"
    }
}
