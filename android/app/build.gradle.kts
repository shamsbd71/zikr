plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.abu.zikr"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.abu.zikr"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        // Stamped at release-build time via -PversionNameOverride= so the
        // running app knows its own version - see release.yml. Never
        // hand-edit this expecting it to track a release; it went stale
        // for multiple releases on the Windows/Linux builds before that
        // was caught, see CLAUDE.md.
        versionName = (project.findProperty("versionNameOverride") as String?) ?: "0.0.0-dev"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
        // Needed for BuildConfig.VERSION_NAME, used by the update
        // checker to know its own version - AGP 8+ no longer generates
        // BuildConfig by default.
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    sourceSets["main"].res.srcDir(layout.buildDirectory.dir("generated/zikr/res"))
    sourceSets["main"].assets.srcDir(layout.buildDirectory.dir("generated/zikr/assets"))
}

// data/zikr.json and data/audio at the repo root are the one list and the
// one set of recitations every platform reads. They are copied into the
// build rather than duplicated in the source tree: a hand-synced copy
// under res/raw is exactly what left this build on 21 stale adhkar while
// the canonical file had 49.
val canonicalData: File = rootProject.file("../data")

val syncZikrList by tasks.registering(Copy::class) {
    from(canonicalData) { include("zikr.json") }
    into(layout.buildDirectory.dir("generated/zikr/res/raw"))
}

// Assets rather than res/raw: a raw resource name has to be a valid Java
// identifier, and "1.mp3" is not. MediaPlayer reads the mp3s directly, so
// unlike the Linux and Windows builds nothing needs converting.
val syncZikrAudio by tasks.registering(Copy::class) {
    from(canonicalData.resolve("audio")) { include("*.mp3") }
    into(layout.buildDirectory.dir("generated/zikr/assets/audio"))
}

tasks.named("preBuild") { dependsOn(syncZikrList, syncZikrAudio) }

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.1")
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    // org.json is stubbed-to-throw in local JVM unit tests by default
    // (the real implementation only exists on-device); this pulls in a
    // real pure-JVM implementation of the same package so ZikrData.parse
    // is genuinely unit-testable rather than always throwing "Stub!".
    testImplementation("org.json:json:20240303")
}
