import combobreaker.buildlogic.AndroidConfig
import combobreaker.buildlogic.libs
import combobreaker.buildlogic.pluginId
import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

@Suppress("unused")
class KmpLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply(libs.pluginId("kotlin-multiplatform"))
                apply(libs.pluginId("android-kmp-library"))
                apply("combo-breaker.code.lint")
            }

            extensions.configure<KotlinMultiplatformExtension> {
                explicitApi()

                applyDefaultHierarchyTemplate()

                targets.withType<KotlinMultiplatformAndroidLibraryTarget>().configureEach {
                    compileSdk = AndroidConfig.COMPILE_SDK
                    minSdk = AndroidConfig.MIN_SDK

                    compilerOptions {
                        jvmTarget.set(JvmTarget.JVM_21)
                    }
                }

                jvm {
                    compilerOptions {
                        jvmTarget.set(JvmTarget.JVM_21)
                    }
                }

                compilerOptions {
                    freeCompilerArgs.add("-Xexpect-actual-classes")
                }
            }
        }
    }
}
