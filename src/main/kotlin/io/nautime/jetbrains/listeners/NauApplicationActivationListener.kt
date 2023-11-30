package io.nautime.jetbrains.listeners

import com.intellij.openapi.application.ApplicationActivationListener
import com.intellij.openapi.components.serviceOrNull
import com.intellij.openapi.wm.IdeFrame
import com.intellij.openapi.wm.impl.welcomeScreen.FlatWelcomeFrame
import com.intellij.openapi.wm.impl.welcomeScreen.WelcomeFrame
import io.nautime.jetbrains.NauPlugin
import io.nautime.jetbrains.model.EventType

class NauApplicationActivationListener : ApplicationActivationListener {

    override fun applicationActivated(ideFrame: IdeFrame) {
//        if(!NauPlugin.isInit()) return
//        KaefPlugin.log.info("applicationActivated ${ideFrame.project?.name}")

        serviceOrNull<NauPlugin>()?.addEventProject(EventType.FRAME_IN, ideFrame.project)
    }

    override fun applicationDeactivated(ideFrame: IdeFrame) {
//        if(!NauPlugin.isInit()) return
//        KaefPlugin.log.info("documentChanged ${ideFrame.project?.name}")

        if(ideFrame is FlatWelcomeFrame || ideFrame is WelcomeFrame) {
            return
        }

        serviceOrNull<NauPlugin>()?.addEventProject(EventType.FRAME_OUT, ideFrame.project)
    }
}
