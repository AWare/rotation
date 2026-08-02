import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
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
    compileSdk = 37

    defaultConfig {
        applicationId = "app.rotatescreen"
        minSdk = 29
        targetSdk = 37
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
        getByName("debug").assets.directories.add("$projectDir/schemas")
        getByName("androidTest").assets.directories.add("$projectDir/schemas")
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

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        freeCompilerArgs.addAll("-opt-in=kotlin.RequiresOptIn")
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
    implementation("androidx.core:core-ktx:1.19.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.11.0")
    implementation("androidx.activity:activity-compose:1.13.0")

    // Compose
    val composeBom = platform("androidx.compose:compose-bom:2026.06.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.9.8")

    // Room
    val roomVersion = "2.8.4"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    // Arrow FP
    val arrowVersion = "2.2.3"
    implementation("io.arrow-kt:arrow-core:$arrowVersion")
    implementation("io.arrow-kt:arrow-fx-coroutines:$arrowVersion")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")

    // Preferences DataStore
    implementation("androidx.datastore:datastore-preferences:1.2.1")

    // Testing
    testImplementation("junit:junit:4.13.2")
    // The test sources import kotlin.test.* assertions; without this they do
    // not compile at all.
    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
    testImplementation("io.mockk:mockk:1.14.11")
    // mockk-android belongs on the instrumented-test classpath only; on the
    // JVM unit-test classpath it shadows the JVM agent with the Android one.
    androidTestImplementation("io.mockk:mockk-android:1.14.11")
    testImplementation("app.cash.turbine:turbine:1.2.1")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("com.google.truth:truth:1.4.5")
    testImplementation("androidx.room:room-testing:$roomVersion")
    // MigrationTestHelper needs InstrumentationRegistry, which Robolectric
    // supplies for JVM unit tests.
    testImplementation("androidx.test:core:1.7.0")
    testImplementation("io.kotest:kotest-assertions-core:6.2.3")
    testImplementation("org.robolectric:robolectric:4.16.1")

    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation(composeBom)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
