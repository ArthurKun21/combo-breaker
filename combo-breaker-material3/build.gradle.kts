plugins {
    id("combo-breaker.android.library")
    id("combo-breaker.android.compose")
    id("combo-breaker.dokka")
    id("combo-breaker.maven.publish")
}

android {
    namespace = "dev.romainguy.text.combobreaker.material3"
}

dependencies {
    implementation(libs.compose.material3)

    api(project(":combo-breaker"))
}
