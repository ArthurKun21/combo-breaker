plugins {
    id("combo-breaker.android.application")
    id("combo-breaker.android.compose")
}

android {
    namespace = "dev.romainguy.text.combobreaker.demo"

    defaultConfig {
        applicationId = "dev.romainguy.text.combobreaker.demo"

        versionCode = 1
        versionName = "1.0"

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)

    implementation(libs.compose.ui)
    implementation(libs.compose.material3)

    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    implementation(libs.pathway)

    implementation(project(":combo-breaker-material3"))
}
