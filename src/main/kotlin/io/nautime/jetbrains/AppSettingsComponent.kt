package io.nautime.jetbrains

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.components.serviceOrNull
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.ui.components.ActionLink
import com.intellij.ui.components.JBTextField
import com.intellij.ui.components.Label
import com.intellij.ui.components.Link
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.TextTransferable
import java.awt.FlowLayout
import javax.swing.JPanel


class AppSettingsComponent {
    val panel: JPanel

    init {
        val formBuilder = FormBuilder.createFormBuilder()

        val nauPlugin = serviceOrNull<NauPlugin>()
        if (nauPlugin == null) {
            formBuilder.addLabeledComponent(
                Label("Visit"),
                Link("Nau dashboard") { BrowserUtil.browse("https://nautime.io/dashboard") }
            )
        } else {
            val pluginState = nauPlugin.getState()

            if (pluginState.isLinked) {
                formBuilder.addLabeledComponent(
                    Label("Plugin is linked. You can explore your working activity on"),
                    Link("Nau dashboard") { BrowserUtil.browse("https://nautime.io/dashboard") }
                )

                formBuilder.addComponent(JPanel(FlowLayout(FlowLayout.LEFT)).apply {
                    add(JBTextField(pluginState.pluginId, 28))
                    add(ActionLink("copy") {
                        CopyPasteManager.getInstance().setContents(TextTransferable(pluginState.pluginId as String?))
                    })
                })
            } else {
                formBuilder.addComponent(JPanel(FlowLayout(FlowLayout.LEFT)).apply {
                    add(Link("Follow link") { BrowserUtil.browse(nauPlugin.getPluginLinkUrl()) })
                    add(Label("to connect Nau plugin"))
                })

                formBuilder.addComponent(JPanel(FlowLayout(FlowLayout.LEFT)).apply {
                    add(Label("Or copy plugin uuid and past it on"))
                    add(Link("Nau dashboard") { BrowserUtil.browse(nauPlugin.getDashboardUrl()) })
                })

                formBuilder.addComponent(JPanel(FlowLayout(FlowLayout.LEFT)).apply {
                    add(JBTextField(pluginState.pluginId, 28))
                    add(ActionLink("copy") {
                        CopyPasteManager.getInstance().setContents(TextTransferable(pluginState.pluginId as String?))
                    })
                })
            }

            formBuilder.addComponentFillVertically(JPanel(), 0)
        }

        panel = formBuilder.panel
    }
}
