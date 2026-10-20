plugins {
    `kotlin-dsl`
}

group = "dev.romainguy.combobreaker.buildlogic"

dependencies {
    compileOnly(libs.android.gradle.plugin)
    compileOnly(libs.kotlin.gradle.plugin)

    implementation(libs.dokka.gradle.plugin)
    implementation(libs.vanniktech.maven.publish.plugin)
}

gradlePlugin {
    plugins {
        register("androidLibrary") {
            id = "combo-breaker.android.library"
            implementationClass = "dev.romainguy.combobreaker.buildlogic.AndroidLibraryConventionPlugin"
        }
        register("androidApplication") {
            id = "combo-breaker.android.application"
            implementationClass = "dev.romainguy.combobreaker.buildlogic.AndroidApplicationConventionPlugin"
        }
        register("androidCompose") {
            id = "combo-breaker.android.compose"
            implementationClass = "dev.romainguy.combobreaker.buildlogic.AndroidComposeConventionPlugin"
        }
        register("dokka") {
            id = "combo-breaker.dokka"
            implementationClass = "dev.romainguy.combobreaker.buildlogic.DokkaConventionPlugin"
        }
        register("mavenPublish") {
            id = "combo-breaker.maven.publish"
            implementationClass = "dev.romainguy.combobreaker.buildlogic.MavenPublishConventionPlugin"
        }
    }
}
