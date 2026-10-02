plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.khuongnd.dexassistant.ui"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.khuongnd.dexassistant.ui"
        minSdk = 30
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
        // Dedicated Fold3 and retained Windows AVD; avoid unused 32-bit native payloads.
        ndk { abiFilters += listOf("arm64-v8a", "x86_64") }

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    sourceSets.getByName("main").assets.srcDir(rootProject.file(".cache/voice/assets"))

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

// Prepare checksum-pinned dependencies explicitly before Gradle/CI; no runtime downloads.
val verifyVoiceAssets by tasks.registering {
    doLast {
        val voiceRoot = rootProject.file(".cache/voice")
        val required = listOf("sherpa-onnx-1.13.8.aar", "assets/kws/tokens.txt",
            "assets/kws/encoder-epoch-12-avg-2-chunk-16-left-64.int8.onnx",
            "assets/kws/decoder-epoch-12-avg-2-chunk-16-left-64.int8.onnx",
            "assets/kws/joiner-epoch-12-avg-2-chunk-16-left-64.int8.onnx")
        check(required.all { voiceRoot.resolve(it).isFile }) {
            "Pinned voice assets missing. Run: python3 scripts/prepare-voice-assets.py"
        }
    }
}
tasks.named("preBuild") { dependsOn(verifyVoiceAssets) }

dependencies {
    implementation(files(rootProject.file(".cache/voice/sherpa-onnx-1.13.8.aar")))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.okhttp)

    testImplementation(libs.junit)
    testImplementation(libs.json)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
