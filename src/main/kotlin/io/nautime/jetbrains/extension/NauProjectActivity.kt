package io.nautime.jetbrains.extension

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import io.nautime.jetbrains.NauPlugin

class NauProjectActivity : ProjectActivity {

    override suspend fun execute(project: Project) {
        service<NauPlugin>().init()

        NauPlugin.log.info("NauProjectActivity Open project ${project.name}")
    }

}
