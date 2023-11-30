package io.nautime.jetbrains.handlers

import com.intellij.codeInsight.editorActions.TypedHandlerDelegate
import com.intellij.openapi.components.serviceOrNull
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import io.nautime.jetbrains.NauPlugin
import io.nautime.jetbrains.model.EventType


class KeyHandler : TypedHandlerDelegate() {

    override fun charTyped(c: Char, project: Project, editor: Editor, file: PsiFile): Result {
        serviceOrNull<NauPlugin>()?.let { nauPlugin ->
            val document: Document = editor.document
            nauPlugin.addEvent(EventType.KEY, project, document)
        }

        return super.charTyped(c, project, editor, file)
    }

}
