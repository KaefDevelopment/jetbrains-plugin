package io.nautime.jetbrains.utils

import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.serviceContainer.AlreadyDisposedException
import io.nautime.jetbrains.model.EventDto
import io.nautime.jetbrains.model.EventType


fun DocumentEvent.toEvent(): EventDto? {
    val file = this.document.getFile() ?: return null
    val project = this.document.getProject() ?: return null

    return EventDto(
        type = EventType.DOCUMENT_CHANGE,
        project = project.name,
        projectBaseDir = project.presentableUrl,
        language = file.extension,
        target = file.presentableUrl, // file.getPath()
        branch = "", // todo
        params = mapOf()
    )
}


fun Document?.getFile(): VirtualFile? {
    if (this == null) return null
    return FileDocumentManager.getInstance().getFile(this)
}

fun Project?.getCurrentFile(): VirtualFile? {
    if (this == null) return null
    return try {
        FileEditorManager.getInstance(this).selectedTextEditor?.document?.getFile()
    } catch (ex: Exception) {
        null
    }
}

fun Project?.orLast() = this ?: lastProject

var lastProject: Project? = null
fun Document.getProject(): Project? {
    val editors = EditorFactory.getInstance().getEditors(this)
    return if (editors.isNotEmpty()) {
        editors[0].project.also {
            lastProject = it
        }
    } else lastProject
}
