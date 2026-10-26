import combobreaker.buildlogic.AndroidConfig
import combobreaker.buildlogic.libs
import combobreaker.buildlogic.pluginId
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

@Suppress("unused")
class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply(libs.pluginId("android-library"))
                apply("combo-breaker.code.lint")
            }

            extensions.configure<LibraryExtension> {
                compileSdk = AndroidConfig.COMPILE_SDK

                defaultConfig {
                    minSdk = AndroidConfig.MIN_SDK

                    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
