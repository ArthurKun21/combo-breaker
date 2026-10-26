plugins {
    id("combo-breaker.android.library")
    id("combo-breaker.compose")
    id("combo-breaker.dokka")
    id("combo-breaker.maven.publish")
}

android {
    namespace = "dev.romainguy.text.combobreaker.material3"
}

kotlin {
    explicitApi()
}

dependencies {
    // Compose types (Modifier, AnnotatedString, TextStyle) appear in this module's public
    // signatures, so they must be exposed to consumers on their compile classpath.
    api(libs.compose.ui)
    api(libs.compose.material3)

    api(project(":combo-breaker"))
}
