package dev.romainguy.combobreaker.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

internal object AndroidConfig {
    const val COMPILE_SDK = 36
    const val TARGET_SDK = 36
    const val MIN_SDK = 29

    val JAVA_VERSION = JavaVersion.VERSION_21
    val JVM_TARGET = JvmTarget.JVM_21
}

internal fun Project.configureAndroidLibrary() {
    extensions.configure<LibraryExtension> {
        compileSdk = AndroidConfig.COMPILE_SDK

        defaultConfig {
            minSdk = AndroidConfig.MIN_SDK
            targetSdk = AndroidConfig.TARGET_SDK

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

    configureKotlin()
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

    configureKotlin()
}

private fun Project.configureKotlin() {
    extensions.configure<KotlinAndroidProjectExtension> {
        compilerOptions {
            jvmTarget.set(AndroidConfig.JVM_TARGET)
        }
    }
}
