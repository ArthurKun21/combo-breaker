import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Applies Dokka. Dokka's plugin mode is selected through the
 * `org.jetbrains.dokka.experimental.gradle.pluginMode` property in `gradle.properties`; the
 * published POM metadata is configured separately in `combobreaker.buildlogic.MavenPublish`.
 */
@Suppress("unused")
class DokkaConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.dokka")
        }
    }
}
