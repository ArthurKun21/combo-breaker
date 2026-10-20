package dev.romainguy.combobreaker.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

internal object AndroidConfig {
    const val COMPILE_SDK = 37
    const val TARGET_SDK = 37
    const val MIN_SDK = 29

    val JAVA_VERSION = JavaVersion.VERSION_21
}

internal fun Project.configureAndroidLibrary() {
    extensions.configure<LibraryExtension> {
        compileSdk = AndroidConfig.COMPILE_SDK

        defaultConfig {
            minSdk = AndroidConfig.MIN_SDK

            testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }

        compileOptions {
            sourceCompatibility = AndroidConfig.JAVA_VERSION
            targetCompatibility = AndroidConfig.JAVA_VERSION
        }

        buildFeatures {
            compose = true
        }
    }
}

internal fun Project.configureAndroidApplication() {
    extensions.configure<ApplicationExtension> {
        compileSdk = AndroidConfig.COMPILE_SDK

        defaultConfig {
            minSdk = AndroidConfig.MIN_SDK
            targetSdk = AndroidConfig.TARGET_SDK
        }

        compileOptions {
            sourceCompatibility = AndroidConfig.JAVA_VERSION
            targetCompatibility = AndroidConfig.JAVA_VERSION
        }

        buildFeatures {
            compose = true
        }
    }
}
