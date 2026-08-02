import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

// Room writes each schema version here. These files are committed: without
// them there is nothing to write a migration against, and no way to test one.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

// Release signing material. Environment variables win over keystore.properties
// so CI can inject secrets without writing a file to the workspace.
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) FileInputStream(file).use { load(it) }
}

fun signingSetting(envName: String, propertyName: String): String? =
    System.getenv(envName) ?: keystoreProperties.getProperty(propertyName)

val releaseStoreFile = signingSetting("KEYSTORE_FILE", "storeFile")
val releaseStorePassword = signingSetting("KEYSTORE_PASSWORD", "storePassword")
val releaseKeyAlias = signingSetting("KEY_ALIAS", "keyAlias")
val releaseKeyPassword = signingSetting("KEY_PASSWORD", "keyPassword")
val hasReleaseSigning = releaseStoreFile != null && releaseStorePassword != null &&
    releaseKeyAlias != null && releaseKeyPassword != null

// Escape hatch for local release smoke-testing only. Never set this in CI: a
// debug keystore is generated per machine, so every build would sign with a
// different certificate and Android would reject the update.
val allowDebugSignedRelease = providers.gradleProperty("allowDebugSignedRelease")
    .map { it.toBoolean() }.getOrElse(false)

// versionCode must increase for every published APK or Android and Obtainium
// refuse to install the update. CI passes BUILD_NUMBER=${{ github.run_number }},
// which is monotonic and, unlike a git commit count, survives shallow clones.
val buildNumber = (System.getenv("BUILD_NUMBER") ?: "0").toIntOrNull() ?: 0
val baseVersionName = "1.0"

android {
    namespace = "app.rotatescreen"
    compileSdk = 34

    defaultConfig {
        applicationId = "app.rotatescreen"
        minSdk = 29
        targetSdk = 34
        versionCode = buildNumber.coerceAtLeast(1)
        versionName = "$baseVersionName.$buildNumber"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    // Migration tests need the exported schemas as *assets*. Robolectric reads
    // the app's merged assets, not the unit-test source set, so they go on the
    // debug build type: visible to tests, absent from the release APK.
    sourceSets {
        getByName("debug").assets.srcDir("$projectDir/schemas")
        getByName("androidTest").assets.srcDir("$projectDir/schemas")
    }

    signingConfigs {
        create("release") {
            when {
                hasReleaseSigning -> {
                    storeFile = rootProject.file(releaseStoreFile!!)
                    storePassword = releaseStorePassword
                    keyAlias = releaseKeyAlias
                    keyPassword = releaseKeyPassword
                }
                allowDebugSignedRelease -> {
                    storeFile = file("${System.getProperty("user.home")}/.android/debug.keystore")
                    storePassword = "android"
                    keyAlias = "androiddebugkey"
                    keyPassword = "android"
                }
                // Otherwise leave unconfigured; packageRelease fails below with
                // an actionable message instead of emitting a debug-signed APK.
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=kotlin.RequiresOptIn",
            "-Xcontext-receivers"
        )
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += setOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "/META-INF/LICENSE.md",
                "/META-INF/LICENSE-notice.md"
            )
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }
}

// Fail the release build rather than shipping an APK signed with a throwaway
// debug key. The debug keystore is generated per machine, so a debug-signed
// release gets a new certificate on every runner and Android rejects the
// update with a signature mismatch.
tasks.matching { it.name == "packageRelease" }.configureEach {
    doFirst {
        check(hasReleaseSigning || allowDebugSignedRelease) {
            """
            Release signing is not configured.

            CI:    set the KEYSTORE_FILE, KEYSTORE_PASSWORD, KEY_ALIAS and
                   KEY_PASSWORD environment variables from repository secrets.
            Local: create keystore.properties (storeFile/storePassword/keyAlias/
                   keyPassword), or pass -PallowDebugSignedRelease=true to build
                   a throwaway debug-signed APK for testing.

            See README.md > Signing.
            """.trimIndent()
        }
    }
}

// Lets the release workflow read the version from Gradle itself rather than
// grepping this file, which previously also matched versionNameSuffix.
tasks.register("printVersion") {
    val name = android.defaultConfig.versionName
    val code = android.defaultConfig.versionCode
    doLast {
        println("versionName=$name")
        println("versionCode=$code")
    }
}

dependencies {
    // AndroidX Core
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")

    // Compose
    val composeBom = platform("androidx.compose:compose-bom:2023.10.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.6")

    // Room
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    // Arrow FP
    val arrowVersion = "1.2.4"
    implementation("io.arrow-kt:arrow-core:$arrowVersion")
    implementation("io.arrow-kt:arrow-fx-coroutines:$arrowVersion")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Preferences DataStore
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    // The test sources import kotlin.test.* assertions; without this they do
    // not compile at all.
    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("io.mockk:mockk:1.13.8")
    // mockk-android belongs on the instrumented-test classpath only; on the
    // JVM unit-test classpath it shadows the JVM agent with the Android one.
    androidTestImplementation("io.mockk:mockk-android:1.13.8")
    testImplementation("app.cash.turbine:turbine:1.0.0")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("com.google.truth:truth:1.1.5")
    testImplementation("androidx.room:room-testing:$roomVersion")
    // MigrationTestHelper needs InstrumentationRegistry, which Robolectric
    // supplies for JVM unit tests.
    testImplementation("androidx.test:core:1.5.0")
    testImplementation("io.kotest:kotest-assertions-core:5.8.0")
    testImplementation("org.robolectric:robolectric:4.11.1")

    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(composeBom)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
