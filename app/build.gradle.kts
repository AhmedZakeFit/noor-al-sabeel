plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

val envKeystoreFile = providers.environmentVariable("NOOR_KEYSTORE_FILE").orNull
val envKeystorePassword = providers.environmentVariable("NOOR_KEYSTORE_PASSWORD").orNull
val envKeyAlias = providers.environmentVariable("NOOR_KEY_ALIAS").orNull
val envKeyPassword = providers.environmentVariable("NOOR_KEY_PASSWORD").orNull
val hasSigningEnv = envKeystoreFile != null && envKeystorePassword != null &&
    envKeyAlias != null && envKeyPassword != null

android {
    namespace = "io.github.ahmedzakefit.noor"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.github.ahmedzakefit.noor"
        minSdk = 24
        targetSdk = 35
        versionCode = 11
        versionName = "1.0.10"
        vectorDrawables { useSupportLibrary = true }
        resourceConfigurations += listOf("en", "ar")
    }

    if (hasSigningEnv) {
        signingConfigs {
            create("releaseEnv") {
                storeFile = file(envKeystoreFile!!)
                storePassword = envKeystorePassword
                keyAlias = envKeyAlias
                keyPassword = envKeyPassword
            }
        }
    }

    buildTypes {
        release {
            // v1.0 keeps shrinking off for reproducibility and easy debugging; see docs/ROADMAP.md
            isMinifyEnabled = false
            if (hasSigningEnv) {
                signingConfig = signingConfigs.getByName("releaseEnv")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}

dependencies {
    implementation(project(":core"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.core)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.coroutines.android)
    implementation(libs.osmdroid.android)
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.org.json)
    debugImplementation(libs.compose.ui.tooling)
}
