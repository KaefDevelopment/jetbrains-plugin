package io.nautime.jetbrains

import com.intellij.openapi.options.Configurable
import org.jetbrains.annotations.Nls
import javax.swing.JComponent


class AppSettingsConfigurable : Configurable {
    private var settingsComponent: AppSettingsComponent? = null

    @Nls(capitalization = Nls.Capitalization.Title)
    override fun getDisplayName(): String {
        return "Nau Time"
    }

    override fun createComponent(): JComponent {
        return AppSettingsComponent().also {
            settingsComponent = it
        }.panel
    }

    override fun isModified(): Boolean {
        return false
    }

    override fun apply() {
    }

    override fun reset() {
    }

    override fun disposeUIResources() {
        settingsComponent = null
    }
}
