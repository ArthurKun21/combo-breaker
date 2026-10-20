plugins {
    id("combo-breaker.android.library")
    id("combo-breaker.android.compose")
    id("combo-breaker.dokka")
    id("combo-breaker.maven.publish")
}

android {
    namespace = "dev.romainguy.text.combobreaker"
}

kotlin {
    explicitApi()
}

dependencies {
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
}
