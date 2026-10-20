plugins {
    id("combo-breaker.android.library")
    id("combo-breaker.android.compose")
    id("combo-breaker.dokka")
    id("combo-breaker.maven.publish")
}

android {
    namespace = "dev.romainguy.text.combobreaker"
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
}
