package io.nautime.jetbrains.listeners

import com.intellij.openapi.components.serviceOrNull
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManagerListener
import io.nautime.jetbrains.NauPlugin
import io.nautime.jetbrains.model.EventType
import io.nautime.jetbrains.utils.getProject

open class NauFileManagerListener() : FileDocumentManagerListener {
    override fun beforeDocumentSaving(document: Document) {
//        KaefPlugin.log.info("FileManagerListener beforeDocumentSaving ${document.getFile()?.name}")
//        if(!NauPlugin.isInit()) return
        serviceOrNull<NauPlugin>()?.addEvent(EventType.DOCUMENT_SAVE, document.getProject(), document)
    }
}
