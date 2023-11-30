package io.nautime.jetbrains.handlers

import com.intellij.codeInsight.editorActions.TypedHandlerDelegate
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import io.nautime.jetbrains.NauPlugin
import io.nautime.jetbrains.model.EventType


class KeyHandler : TypedHandlerDelegate() {


    override fun charTyped(c: Char, project: Project, editor: Editor, file: PsiFile): Result {
        if(!NauPlugin.isInit()) return super.charTyped(c, project, editor, file)

        val document: Document = editor.document

        NauPlugin.getInstance().addEvent(EventType.KEY, project, document) // , mapOf("key" to c.toString())

        return super.charTyped(c, project, editor, file)
    }


}
