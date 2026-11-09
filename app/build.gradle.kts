plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "ir.unilink.android"
    compileSdk = 35

    defaultConfig {
        applicationId = "ir.unilink.android"
        minSdk = 26
        targetSdk = 35
        versionCode = 5
        versionName = "1.3.0"

        // Drop translations of libraries we don't ship a UI for (the app itself is en + fa).
        @Suppress("DEPRECATION")
        resourceConfigurations += listOf("en", "fa")
    }

    // A fixed (committed) debug keystore keeps the APK signature identical across
    // CI runs, so a new build installs over the old one without uninstalling first.
    signingConfigs {
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            // R8 shrinks and optimizes code, and unused resources are removed: smaller, faster APK.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Signed with the committed key so new builds install over old ones (personal use).
            signingConfig = signingConfigs.getByName("debug")
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
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.09.03"))
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    // Installs the baseline profiles that ship with Compose, so the first launch is faster.
    implementation("androidx.profileinstaller:profileinstaller:1.3.1")
}
