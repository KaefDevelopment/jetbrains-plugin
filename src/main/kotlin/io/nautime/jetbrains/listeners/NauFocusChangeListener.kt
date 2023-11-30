package io.nautime.jetbrains.listeners

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.FocusChangeListener
import io.nautime.jetbrains.NauPlugin
import io.nautime.jetbrains.model.EventType
import java.awt.event.FocusEvent

class NauFocusChangeListener : FocusChangeListener {

    override fun focusGained(editor: Editor, event: FocusEvent) {
        if(!NauPlugin.isInit()) return
        NauPlugin.getInstance().addEvent(EventType.DOCUMENT_FOCUS, editor.project, editor.document)
    }

}
