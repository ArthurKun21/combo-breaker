package dev.romainguy.combobreaker.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply(libs.pluginId("android-application"))

            configureAndroidApplication()
        }
    }
}
