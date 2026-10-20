package com.github.arthurkun21

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Applies the Compose compiler plugin. The `compose` build feature itself is enabled by the
 * Android application/library convention plugins.
 */
class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply(libs.pluginId("kotlin-compose"))
        }
    }
}
