package combobreaker.buildlogic

import org.gradle.api.JavaVersion as GradleJavaVersion

object AndroidConfig {
    const val COMPILE_SDK = 37
    const val COMPILE_SDK_MINOR = 1
    const val TARGET_SDK = 37
    const val MIN_SDK = 29

    // https://youtrack.jetbrains.com/issue/KT-66995/JvmTarget-and-JavaVersion-compatibility-for-easier-JVM-version-setup
    val JavaVersion = GradleJavaVersion.VERSION_21
}
