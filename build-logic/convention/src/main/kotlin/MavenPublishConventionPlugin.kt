import combobreaker.buildlogic.configureMavenPublish
import org.gradle.api.Plugin
import org.gradle.api.Project

@Suppress("unused")
class MavenPublishConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            // Group and version must be set before the plugin is applied: the publishing
            // plugin derives the publication coordinates from the project properties and
            // finalizes them during apply
            group = providers.gradleProperty("GROUP").get()
            version = providers.environmentVariable("RELEASE_TAG")
                .map { it.removePrefix("v") }
                .orElse(providers.gradleProperty("VERSION_NAME"))
                .get()

            pluginManager.apply("com.vanniktech.maven.publish")

            configureMavenPublish()
        }
    }
}
