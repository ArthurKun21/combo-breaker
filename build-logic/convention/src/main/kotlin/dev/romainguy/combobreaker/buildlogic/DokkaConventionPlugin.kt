package dev.romainguy.combobreaker.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Applies Dokka. Documentation settings are controlled through `gradle.properties`
 * (POM_* properties and `org.jetbrains.dokka.experimental.gradle.pluginMode`).
 */
class DokkaConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.dokka")
        }
    }
}
