package io.nautime.jetbrains.listeners

import com.intellij.openapi.application.ApplicationActivationListener
import com.intellij.openapi.wm.IdeFrame
import com.intellij.openapi.wm.impl.welcomeScreen.FlatWelcomeFrame
import com.intellij.openapi.wm.impl.welcomeScreen.WelcomeFrame
import io.nautime.jetbrains.NauPlugin
import io.nautime.jetbrains.model.EventType

class NauApplicationActivationListener : ApplicationActivationListener {

    override fun applicationActivated(ideFrame: IdeFrame) {
//        KaefPlugin.log.info("applicationActivated ${ideFrame.project?.name}")

        NauPlugin.getInstance().addEventProject(EventType.FRAME_IN, ideFrame.project)
    }

    override fun applicationDeactivated(ideFrame: IdeFrame) {
//        KaefPlugin.log.info("documentChanged ${ideFrame.project?.name}")

        if(ideFrame is FlatWelcomeFrame || ideFrame is WelcomeFrame) {
            return
        }

        NauPlugin.getInstance().addEventProject(EventType.FRAME_OUT, ideFrame.project)
    }
}
