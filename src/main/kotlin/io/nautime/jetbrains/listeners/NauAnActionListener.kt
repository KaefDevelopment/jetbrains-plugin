package io.nautime.jetbrains.listeners

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ex.AnActionListener
import com.intellij.openapi.components.serviceOrNull
import io.nautime.jetbrains.NauPlugin
import io.nautime.jetbrains.model.EventParamsMap
import io.nautime.jetbrains.model.EventType
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent

class NauAnActionListener : AnActionListener {

    override fun beforeActionPerformed(action: AnAction, event: AnActionEvent) {
        //        NauPlugin.log.info("beforeActionPerformed $action ${event.inputEvent}")

        serviceOrNull<NauPlugin>()?.let { nauPlugin ->
            val inputEvent = event.inputEvent
            if (inputEvent !is MouseEvent && inputEvent !is KeyEvent) return

            nauPlugin.addEventProject(
                type = EventType.ACTION,
                project = event.project,
                params = calculateParams(event)
            )
        }
    }

    private fun calculateParams(event: AnActionEvent): EventParamsMap {
        val inputEvent = event.inputEvent
        if (inputEvent is KeyEvent) {
            return mapOf(
                "keyCode" to inputEvent.keyCode.toString()
            )
        }

        return emptyMap()
    }
}
