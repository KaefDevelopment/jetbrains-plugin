package io.nautime.jetbrains

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.components.serviceOrNull
import com.intellij.ui.components.ActionLink
import com.intellij.ui.components.Label
import com.intellij.ui.components.Link
import com.intellij.util.ui.FormBuilder
import java.awt.FlowLayout
import javax.swing.JPanel


class AppSettingsComponent {
    var panel: JPanel

    init {
        val formBuilder = initForm()
        panel = formBuilder.panel
    }

    private fun initForm(): FormBuilder {
        val formBuilder = FormBuilder.createFormBuilder()

        val nauPlugin = serviceOrNull<NauPlugin>()
        if (nauPlugin == null) {
            formBuilder.addLabeledComponent(
                Label("Visit"),
                Link("Nau dashboard") { BrowserUtil.browse("$SERVER_ADDRESS/dashboard") }
            )
        } else {
            val pluginState = nauPlugin.getState()

            if (pluginState.isLinked) {
                formBuilder.addLabeledComponent(
                    Label("Plugin is linked. You can explore your working activity on"),
                    Link("Nau dashboard") { BrowserUtil.browse("$SERVER_ADDRESS/dashboard") }
                )

                //                formBuilder.addComponent(JPanel(FlowLayout(FlowLayout.LEFT)).apply {
                //                    Label("Plugin id: ")
                //                    add(JBTextField(pluginState.pluginId, 28))
                //                    add(ActionLink("copy") {
                //                        CopyPasteManager.getInstance().setContents(TextTransferable(pluginState.pluginId as String?))
                //                    })
                //                })

                formBuilder.addComponent(JPanel(FlowLayout(FlowLayout.LEFT)).apply {
                    add(ActionLink("unlink") {
                        nauPlugin.unlink()
                    })
                })
            } else {
                formBuilder.addComponent(JPanel(FlowLayout(FlowLayout.LEFT)).apply {
                    add(Link("Follow link") { BrowserUtil.browse(nauPlugin.getPluginLinkUrl()) })
                    add(Label("to connect Nau plugin"))
                })

                //                formBuilder.addComponent(JPanel(FlowLayout(FlowLayout.LEFT)).apply {
                //                    add(Label("Or copy plugin uuid and past it on"))
                //                    add(Link("Nau dashboard") { BrowserUtil.browse(nauPlugin.getDashboardUrl()) })
                //                })
                //
                //                formBuilder.addComponent(JPanel(FlowLayout(FlowLayout.LEFT)).apply {
                //                    add(JBTextField(pluginState.pluginId, 28))
                //                    add(ActionLink("copy") {
                //                        CopyPasteManager.getInstance().setContents(TextTransferable(pluginState.pluginId as String?))
                //                    })
                //                })
            }

            formBuilder.addComponentFillVertically(JPanel(), 0)
        }
        return formBuilder
    }
}
