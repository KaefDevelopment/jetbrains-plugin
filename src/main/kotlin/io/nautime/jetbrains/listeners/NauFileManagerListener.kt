package io.nautime.jetbrains.listeners

import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManagerListener
import io.nautime.jetbrains.NauPlugin
import io.nautime.jetbrains.model.EventType
import io.nautime.jetbrains.utils.getProject

open class NauFileManagerListener() : FileDocumentManagerListener {
    override fun beforeDocumentSaving(document: Document) {
//        KaefPlugin.log.info("FileManagerListener beforeDocumentSaving ${document.getFile()?.name}")
        NauPlugin.getInstance().addEvent(EventType.DOCUMENT_SAVE, document.getProject(), document)
    }
}
