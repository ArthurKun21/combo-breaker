package com.github.arthurkun21

import org.gradle.api.Plugin
import org.gradle.api.Project

class MavenPublishConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            group = providers.gradleProperty("GROUP").get()

            pluginManager.apply("com.vanniktech.maven.publish")

            configureMavenPublish()
        }
    }
}
