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
class KmpSampleConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply(libs.pluginId("kotlin-multiplatform"))
                apply(libs.pluginId("android-kmp-library"))
                apply("combo-breaker.compose.multiplatform")
                apply("combo-breaker.code.lint")
            }

            extensions.configure<KotlinMultiplatformExtension> {
                applyDefaultHierarchyTemplate()

                targets.withType<KotlinMultiplatformAndroidLibraryTarget>().configureEach {
                    compileSdk = AndroidConfig.COMPILE_SDK
                    minSdk = AndroidConfig.MIN_SDK

                    // Compose Multiplatform resources need Android resources enabled on the
                    // AGP KMP android target.
                    androidResources {
                        enable = true
                    }

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
