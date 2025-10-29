package com.oberdiah.deepcomplexity.actions

import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity

class OnStartup : StartupActivity {
    override fun runActivity(project: Project) {
        // Wait until the indexing process is complete
        DumbService.getInstance(project).runReadActionInSmartMode {
            // Get the index

            thisLogger().warn("Indexing complete")
        }
    }
}