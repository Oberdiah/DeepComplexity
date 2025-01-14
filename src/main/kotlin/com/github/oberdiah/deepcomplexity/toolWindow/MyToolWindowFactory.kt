package com.github.oberdiah.deepcomplexity.toolWindow

import com.github.oberdiah.deepcomplexity.MyBundle
import com.github.oberdiah.deepcomplexity.indexes.PRIMARY_INDEX_ID
import com.github.oberdiah.deepcomplexity.services.MyProjectService
import com.github.oberdiah.deepcomplexity.staticAnalysis.MethodProcessing
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.content.ContentFactory
import com.intellij.util.indexing.FileBasedIndex
import javax.swing.JButton


class MyToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val myToolWindow = MyToolWindow(toolWindow)
        val content = ContentFactory.getInstance().createContent(myToolWindow.getContent(), null, false)
        toolWindow.contentManager.addContent(content)
    }

    override fun shouldBeAvailable(project: Project) = true

    class MyToolWindow(val toolWindow: ToolWindow) {
        private val service = toolWindow.project.service<MyProjectService>()

        fun getContent() = JBPanel<JBPanel<*>>().apply {
            val textArea = JBTextArea(MyBundle.message("randomLabel", "?"))

            add(textArea)

            add(JButton("Build Tree").apply {
                addActionListener {
                    scanMethod(false)
                }
            })

            add(JButton("Build & Eval Tree").apply {
                addActionListener {
                    scanMethod(true)
                }
            })
        }

        private fun scanMethod(evaluateResults: Boolean) {
            val fileEditorManager = FileEditorManager.getInstance(toolWindow.project)

            // Get the currently selected file
            val selectedTextEditor = fileEditorManager.selectedTextEditor

            selectedTextEditor?.let { editor ->
                val psiFile = PsiDocumentManager.getInstance(toolWindow.project)
                    .getPsiFile(editor.document)

                val offset = editor.caretModel.offset
                val element = psiFile?.findElementAt(offset)

                if (element != null && element.parent is PsiMethod) {
                    MethodProcessing.processMethod(element.parent as PsiMethod, evaluateResults)
                }
            }
        }
    }
}
