plugins {
    `kotlin-dsl`
}

group = "combobreaker.buildlogic"

dependencies {
    implementation(libs.android.gradle.plugin)
    implementation(libs.compose.compiler.gradle)
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.spotless.gradle)
    implementation(libs.dokka.gradle.plugin)
    implementation(libs.vanniktech.maven.publish.plugin)
}

gradlePlugin {
    plugins {
        register("codeLint") {
            id = "combo-breaker.code.lint"
            implementationClass = "CodeLintConventionPlugin"
        }
        register("compose") {
            id = "combo-breaker.compose"
            implementationClass = "ComposeConventionPlugin"
        }
        register("androidLibrary") {
            id = "combo-breaker.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("kmpLibrary") {
            id = "combo-breaker.kotlin.multiplatform"
            implementationClass = "KmpLibraryConventionPlugin"
        }
        register("androidApplication") {
            id = "combo-breaker.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("dokka") {
            id = "combo-breaker.dokka"
            implementationClass = "DokkaConventionPlugin"
        }
        register("mavenPublish") {
            id = "combo-breaker.maven.publish"
            implementationClass = "MavenPublishConventionPlugin"
        }
    }
}
