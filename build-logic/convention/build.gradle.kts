plugins {
    `kotlin-dsl`
}

group = "com.github.arthurkun21"

dependencies {
    compileOnly(libs.android.gradle.plugin)

    implementation(libs.dokka.gradle.plugin)
    implementation(libs.vanniktech.maven.publish.plugin)
}

gradlePlugin {
    plugins {
        register("androidLibrary") {
            id = "combo-breaker.android.library"
            implementationClass = "com.github.arthurkun21.AndroidLibraryConventionPlugin"
        }
        register("androidApplication") {
            id = "combo-breaker.android.application"
            implementationClass = "com.github.arthurkun21.AndroidApplicationConventionPlugin"
        }
        register("androidCompose") {
            id = "combo-breaker.android.compose"
            implementationClass = "com.github.arthurkun21.AndroidComposeConventionPlugin"
        }
        register("dokka") {
            id = "combo-breaker.dokka"
            implementationClass = "com.github.arthurkun21.DokkaConventionPlugin"
        }
        register("mavenPublish") {
            id = "combo-breaker.maven.publish"
            implementationClass = "com.github.arthurkun21.MavenPublishConventionPlugin"
        }
    }
}
