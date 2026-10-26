import combobreaker.buildlogic.AndroidConfig
import combobreaker.buildlogic.libs
import combobreaker.buildlogic.pluginId
import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

@Suppress("unused")
class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply(libs.pluginId("android-application"))
                apply("combo-breaker.code.lint")
            }

            extensions.configure<ApplicationExtension> {
                compileSdk = AndroidConfig.COMPILE_SDK

                defaultConfig {
                    minSdk = AndroidConfig.MIN_SDK
                    targetSdk = AndroidConfig.TARGET_SDK
                }

                compileOptions {
                    sourceCompatibility = AndroidConfig.JavaVersion
                    targetCompatibility = AndroidConfig.JavaVersion
                }

                buildFeatures {
                    compose = true
                }
            }
        }
    }
}
