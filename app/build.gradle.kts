plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// ---- Version lives here. Bump both for every release. ----
val jotVersionName = "0.2.0"
val jotVersionCode = 2
// ----------------------------------------------------------

val keystorePath: String? = System.getenv("JOT_KEYSTORE")?.takeIf { it.isNotBlank() }

android {
    namespace = "com.gentlemanjoel.jot"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.gentlemanjoel.jot"
        minSdk = 29
        targetSdk = 35
        versionCode = jotVersionCode
        versionName = jotVersionName

        // Point the in-app updater at your repo.
        buildConfigField("String", "GITHUB_REPO", "\"GentlemanJoel/Jot\"")
    }

    if (keystorePath != null) {
        signingConfigs {
            create("release") {
                storeFile = file(keystorePath)
                storePassword = System.getenv("JOT_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("JOT_KEY_ALIAS")
                keyPassword = System.getenv("JOT_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            signingConfig = if (keystorePath != null) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources.excludes += setOf("/META-INF/{AL2.0,LGPL2.1}")
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
}
