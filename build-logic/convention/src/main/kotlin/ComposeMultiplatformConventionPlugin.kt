import combobreaker.buildlogic.libs
import combobreaker.buildlogic.pluginId
import org.gradle.api.Plugin
import org.gradle.api.Project

@Suppress("unused")
class ComposeMultiplatformConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("combo-breaker.compose")
                apply(libs.pluginId("jetbrains-compose"))
            }
        }
    }
}
