import combobreaker.buildlogic.configureMavenPublish
import org.gradle.api.Plugin
import org.gradle.api.Project

@Suppress("unused")
class MavenPublishConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            group = providers.gradleProperty("GROUP").get()

            pluginManager.apply("com.vanniktech.maven.publish")

            configureMavenPublish()
        }
    }
}
