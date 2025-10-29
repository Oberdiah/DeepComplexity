package com.oberdiah.deepcomplexity.services

import com.oberdiah.deepcomplexity.MyBundle
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import org.jetbrains.uast.UFile

@Service(Service.Level.PROJECT)
class MyProjectService(project: Project) {

    init {
        thisLogger().info(MyBundle.message("projectService", project.name))
    }

    fun getRandomNumber() = (1..100).random()

    fun addToIndex(file: UFile) {
        // Add the file to the index

//        println("Added file to index: ${file.allClasses().firstOrNull()?.name}")
    }

    // Effectively the entry point for the entire plugin.
    fun processFile(file: UFile) {

    }
}
