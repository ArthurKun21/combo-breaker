package dev.romainguy.combobreaker.buildlogic

import com.vanniktech.maven.publish.MavenPublishBaseExtension
import com.vanniktech.maven.publish.SonatypeHost
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * Applies and configures the Maven Central publishing plugin. Coordinates and POM metadata come
 * from the properties declared in `gradle.properties`.
 */
class MavenPublishConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            group = providers.gradleProperty("GROUP").get()
            version = providers.gradleProperty("VERSION_NAME").get()

            pluginManager.apply("com.vanniktech.maven.publish")

            extensions.configure<MavenPublishBaseExtension> {
                publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
                signAllPublications()
            }
        }
    }
}
