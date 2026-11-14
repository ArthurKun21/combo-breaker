plugins {
    id("combo-breaker.kotlin.multiplatform")
    id("combo-breaker.compose")
    id("combo-breaker.dokka")
    id("combo-breaker.maven.publish")
}

kotlin {
    android {
        namespace = "dev.romainguy.text.combobreaker"
    }

    sourceSets {
        commonMain.dependencies {
            // Compose types (Modifier, TextStyle, Alignment, Path, ...) appear in this module's public
            // signatures, so they must be exposed to consumers on their compile classpath.
            api(libs.compose.ui)

            implementation(libs.compose.material3)
        }
    }
}
