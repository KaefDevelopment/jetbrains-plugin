package io.nautime.jetbrains.listeners

import com.intellij.openapi.components.serviceOrNull
import com.intellij.openapi.editor.event.BulkAwareDocumentListener
import com.intellij.openapi.editor.event.DocumentEvent
import io.nautime.jetbrains.NauPlugin
import io.nautime.jetbrains.utils.toEvent

open class NauDocumentListener() : BulkAwareDocumentListener {

    override fun documentChanged(event: DocumentEvent) {
//        NauPlugin.log.info("documentChanged ${event.document.getFile()?.name}")
//        if(!NauPlugin.isInit()) return
        serviceOrNull<NauPlugin>()?.addEvent(event.toEvent())
    }

    override fun documentChangedNonBulk(event: DocumentEvent) {
//        NauPlugin.log.info("documentChangedNonBulk ${event.document.getFile()?.name}")
//        if(!NauPlugin.isInit()) return
        serviceOrNull<NauPlugin>()?.addEvent(event.toEvent())
    }
}
