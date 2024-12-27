package com.github.oberdiah.deepcomplexity.toolWindow

import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBPanel
import com.intellij.ui.content.ContentFactory
import com.github.oberdiah.deepcomplexity.MyBundle
import com.github.oberdiah.deepcomplexity.indexes.PRIMARY_INDEX_ID
import com.github.oberdiah.deepcomplexity.services.MyProjectService
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.ui.components.JBTextArea
import com.intellij.util.indexing.FileBasedIndex
import com.sun.java.accessibility.util.AWTEventMonitor.addActionListener
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
            add(JButton("Reload Indices!").apply {
                addActionListener {
                    FileBasedIndex.getInstance().requestRebuild(PRIMARY_INDEX_ID)
                    println("Reloading Indices :)")
                }
            })

            add(JButton("Scan").apply {
                addActionListener {
                    val fileEditorManager = FileEditorManager.getInstance(toolWindow.project)

                    // Get the currently selected file
                    val selectedTextEditor = fileEditorManager.selectedTextEditor
                    val currentFile = selectedTextEditor?.virtualFile

                    if (currentFile != null) {
                        val textBuffer = StringBuilder()


                        textArea.text = textBuffer.toString()
                    } else {
                        textArea.text = "No file selected!"
                    }
                }
            })
        }
    }
}
