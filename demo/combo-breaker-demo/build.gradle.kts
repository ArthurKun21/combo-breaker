plugins {
    id("combo-breaker.kotlin.multiplatform.sample")
}

kotlin {
    android {
        namespace = "dev.romainguy.text.combobreaker.demo.shared"
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":combo-breaker-material3"))

            implementation(compose.components.resources)
        }

        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
        }
    }
}

compose.desktop.application {
    mainClass = "io.github.arthurkun21.text.combobreaker.demo.MainKt"
}

compose.resources {
    packageOfResClass = "io.github.arthurkun21.text.combobreaker.demo"
}
