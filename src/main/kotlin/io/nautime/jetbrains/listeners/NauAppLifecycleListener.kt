package io.nautime.jetbrains.listeners

import com.intellij.ide.AppLifecycleListener
import com.intellij.openapi.components.serviceOrNull
import io.nautime.jetbrains.NauPlugin

class NauAppLifecycleListener : AppLifecycleListener {

    override fun appFrameCreated(commandLineArgs: MutableList<String>) {
        serviceOrNull<NauPlugin>()?.init()

//        KaefPlugin.getInstance().addEventProject(EventType.APP_INIT)
    }

}
