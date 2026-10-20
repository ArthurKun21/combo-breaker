package com.github.arthurkun21

import org.gradle.api.Plugin
import org.gradle.api.Project

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply(libs.pluginId("android-library"))

            configureAndroidLibrary()

            // AGP registers the variant compile tasks during its own afterEvaluate callback,
            // which was queued before this one, so the tasks exist by the time this runs.
            afterEvaluate { configureAbiValidation() }
        }
    }
}
