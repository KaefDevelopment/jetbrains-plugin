package io.nautime.jetbrains.listeners

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.vfs.VirtualFile
import io.nautime.jetbrains.NauPlugin
import io.nautime.jetbrains.model.EventType

class NauFileEditorManagerListener : FileEditorManagerListener {

    override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
//        KaefPlugin.log.info("fileOpened ${file.name}")
        NauPlugin.getInstance().addEvent(EventType.DOCUMENT_OPEN, source.project, file)
    }
}
