plugins {
    id("combo-breaker.kotlin.multiplatform")
    id("combo-breaker.compose")
    id("combo-breaker.dokka")
    id("combo-breaker.maven.publish")
}

kotlin {
    android {
        namespace = "dev.romainguy.text.combobreaker.material3"
    }

    sourceSets {
        commonMain.dependencies {
            // Compose types (Modifier, AnnotatedString, TextStyle) appear in this module's public
            // signatures, so they must be exposed to consumers on their compile classpath.
            api(libs.compose.ui)
            api(libs.compose.material3)

            api(project(":combo-breaker"))
        }
    }
}
