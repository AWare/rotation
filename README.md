# Rotation Control

> **⚠️ WARNING: This app is vibe-coded and not intended for consumption.**
>
> This codebase was developed through experimental "vibe coding" - an approach prioritizing aesthetic and conceptual exploration over production readiness. It may contain unconventional patterns, incomplete error handling, or undocumented behavior. Use at your own risk for educational or experimental purposes only.

A minimal Android app for controlling screen orientation using functional programming with Kotlin and Arrow. Focused on simplicity, testability, and type-safe error handling.

## Features

- **Quick Settings Tile**: Tap to cycle through orientations
- **Minimal UI**: Simple list interface with tap-to-change controls
- **Per-App Settings**: Automatic orientation switching per app
- **Multi-Screen Support**: Target specific displays
- **Comprehensive Tests**: Extensive unit test coverage for all FP logic
- **Type-Safe**: Arrow Either for error handling without exceptions
- **Persistent Storage**: Room database + DataStore

## Supported Orientations

Auto Rotate • Portrait • Landscape • Reverse Portrait • Reverse Landscape • Sensor

## Architecture

The app is built using modern Android development practices and functional programming:

### Tech Stack

- **Kotlin 1.9.21** with functional programming patterns
- **Arrow 1.2.1** for FP types (Either, Option)
- **Jetpack Compose** with Material 3
- **Room 2.6.1** for database
- **DataStore** for preferences
- **MVVM** with reactive StateFlow

### Project Structure

```
app/src/
├── main/java/com/aware/rotation/
│   ├── domain/model/           # FP domain models (immutable)
│   ├── data/
│   │   ├── local/              # Room database
│   │   ├── repository/         # Either-based repository
│   │   └── preferences/        # DataStore
│   ├── service/                # Android services
│   ├── tile/                   # Quick Settings tile
│   ├── ui/                     # Minimal Compose UI
│   └── util/                   # FP utilities
└── test/java/                  # Comprehensive unit tests
```

## Functional Programming Patterns

```kotlin
// Type-safe error handling with Either
fun setOrientation(orientation: ScreenOrientation): Either<OrientationError, Unit> = either {
    PermissionChecker.checkWriteSettingsPermission(context).bind()
    applyOrientation(orientation).bind()
}

// Immutable state transformations
fun withOrientation(newOrientation: ScreenOrientation): AppOrientationSetting =
    copy(orientation = newOrientation)

// Pure functions and composition
val effectiveOrientation = state.getEffectiveOrientation(packageName)
```

## Testing

Comprehensive test coverage with focus on:
- Domain model immutability and transformations
- Either-based error handling
- Repository operations
- State management with Flow
- ViewModel reactive behavior

### Run Tests

```bash
./gradlew test                  # All unit tests
./gradlew testDebugUnitTest    # Debug tests only
```

### Test Libraries

- JUnit 4
- MockK for mocking
- Turbine for Flow testing
- Coroutines Test
- Kotest assertions
- Truth assertions

## Setup & Permissions

### Required Permissions

1. **WRITE_SETTINGS**: Required to modify system orientation settings
   - The app will prompt to grant this permission on first launch

2. **Accessibility Service**: Required for per-app orientation control
   - Detects which app is in the foreground
   - No data is collected or transmitted
   - Settings → Accessibility → Rotation Control

### Installation

```bash
# Build debug APK
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug
```

Or download from [GitHub Releases](../../releases)

## Usage

### Quick Settings Tile

1. Add the "Screen Rotation" tile to Quick Settings
2. Tap to cycle through orientations:
   - Auto Rotate → Portrait → Landscape → Reverse Portrait → Reverse Landscape → Sensor

### In-App Control

- **Global**: Tap to cycle through orientations
- **Per-App**: Enable Accessibility, then tap any app to set orientation
- **Search**: Filter apps by name

## CI/CD

### GitHub Actions

**Build & Test** (on push to `main`/`claude/**` and on PRs):
- Runs unit tests, lint and a debug APK build
- Any of those failing fails the job
- Uploads test/lint reports and the debug APK

**Release** (on push to `main`, or manually via *Run workflow*):
- Runs the unit tests
- Builds a release APK signed with the release keystore
- Refuses to publish a debug-signed APK
- Creates a GitHub release tagged `v<versionName>`

### Versioning

`versionCode` and `versionName` come from `BUILD_NUMBER`, which CI sets to
`github.run_number`. Every release therefore gets a higher `versionCode` than
the last, which is what Android and Obtainium require to offer an update. Local
builds default to `BUILD_NUMBER=0` (version `1.0.0`, code `1`).

### Signing (required for releases)

Releases must be signed with a stable key. Android refuses to install an update
whose signing certificate differs from the installed app's, so a key that
changes between builds means every update fails until the user uninstalls.

Generate a keystore once and keep it safe — losing it means no existing install
can ever be updated.

If you use 1Password, `tools/setup-release-signing.sh` does the whole thing:
generates the password in 1Password, creates the keystore, stores the keystore
file in 1Password as a backup, and loads all four GitHub Actions secrets. The
password is never printed and never passed as a command-line argument.

```bash
./tools/setup-release-signing.sh
```

Otherwise, by hand:

```bash
keytool -genkeypair -v -keystore release.jks -alias rotation \
  -keyalg RSA -keysize 2048 -validity 10000
base64 -w0 release.jks   # value for the KEYSTORE_BASE64 secret
```

Add these repository secrets:
- `KEYSTORE_BASE64`: base64-encoded keystore file
- `KEYSTORE_PASSWORD`: keystore password
- `KEY_ALIAS`: key alias
- `KEY_PASSWORD`: key password

For a local release build, either create `keystore.properties` in the repo root:

```properties
storeFile=release.jks
storePassword=...
keyAlias=rotation
keyPassword=...
```

…or pass `-PallowDebugSignedRelease=true` for a throwaway debug-signed APK.
Never use that flag for anything you intend to distribute.

## Development

### Code Style

- Immutable data structures (data classes with val)
- Pure functions for transformations
- Either for error handling (no exceptions)
- Flow for reactive streams
- Small, composable functions

### Adding Features

1. Define immutable domain models
2. Create repository methods with Either return types
3. Add comprehensive unit tests
4. Update ViewModel with StateFlow
5. Minimal UI updates

## Database Schema

```kotlin
@Entity(tableName = "app_orientations")
data class AppOrientationEntity(
    @PrimaryKey val packageName: String,
    val appName: String,
    val orientationValue: Int,
    val targetScreenId: Int,
    val targetScreenName: String,
    val enabled: Boolean,
    val lastModified: Long
)
```

## Building

```bash
./gradlew assembleDebug        # Debug build
./gradlew assembleRelease      # Release build (needs signing config, see above)
./gradlew testDebugUnitTest    # Run unit tests
./gradlew lintDebug            # Run lint
./gradlew -q printVersion      # Show the version this build would produce
```

### Local toolchain

The build needs JDK 17 and the Android SDK (platform 34, build-tools 34.0.0).
Neither has to come from Android Studio; a headless setup is enough to run
everything CI runs:

```bash
# JDK 17
mkdir -p ~/.jdks/temurin-17
curl -fSL "https://api.adoptium.net/v3/binary/latest/17/ga/linux/x64/jdk/hotspot/normal/eclipse" \
  | tar -xz -C ~/.jdks/temurin-17 --strip-components=1

# Android SDK command-line tools
mkdir -p ~/Android/Sdk/cmdline-tools
curl -fSLo /tmp/cmdline-tools.zip \
  "https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"
unzip -q /tmp/cmdline-tools.zip -d ~/Android/Sdk/cmdline-tools
mv ~/Android/Sdk/cmdline-tools/cmdline-tools ~/Android/Sdk/cmdline-tools/latest

export JAVA_HOME="$HOME/.jdks/temurin-17"
export ANDROID_HOME="$HOME/Android/Sdk"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH"

yes | sdkmanager --licenses
sdkmanager --install "platform-tools" "platforms;android-34" "build-tools;34.0.0"
```

Gradle finds the SDK through `ANDROID_HOME`, so no `local.properties` is needed.

## Requirements

- Android 10 (API 29) or higher
- Device with orientation sensors
- **WRITE_SETTINGS permission** (must be granted manually)

## Troubleshooting

### Orientation Not Changing

If the app builds successfully but doesn't change the screen orientation:

1. **Check Permission**
   - Open the app and verify permission warnings are not showing
   - Go to Settings → Apps → Rotation → Special app access → Modify system settings
   - Make sure "Allow modifying system settings" is **enabled**

2. **Check Logs**
   - Connect device via ADB: `adb logcat | grep OrientationControl`
   - Look for:
     - `WRITE_SETTINGS permission not granted!` → Permission issue
     - `Successfully updated system settings` → Settings updated (but might not work on all devices)
     - Toast notifications showing errors

3. **Test with Quick Settings Tile**
   - Add the "Rotation" tile to Quick Settings
   - Tap it and watch for Toast messages
   - Check logcat for detailed logging

4. **Known Limitations**
   - Some Android ROMs/manufacturers block orientation control from regular apps
   - On some devices, `Settings.System.USER_ROTATION` only works when auto-rotate is disabled
   - System apps or root access may be required on some devices
   - Per-app orientation requires Accessibility Service permission

5. **Test Manually**
   - Try disabling auto-rotate in system settings first
   - Then use the app to set a specific orientation
   - Check if `adb shell settings get system user_rotation` changes

### Debugging Steps

```bash
# Check if permission is granted
adb shell dumpsys package com.aware.rotation | grep WRITE_SETTINGS

# Watch logs while using the app
adb logcat -s OrientationControl:D

# Check current rotation settings
adb shell settings get system accelerometer_rotation
adb shell settings get system user_rotation

# Manually test setting rotation (as comparison)
adb shell settings put system accelerometer_rotation 0
adb shell settings put system user_rotation 1  # landscape
```

### Expected Log Output

When working correctly, you should see:
```
D/OrientationControl: handleIntent: action=com.aware.rotation.action.SET_ORIENTATION
D/OrientationControl: SET_ORIENTATION: orientationValue=1, screenId=-1
D/OrientationControl: Setting orientation to: Portrait
D/OrientationControl: setOrientationForAllDisplays: Portrait
D/OrientationControl: Setting ACCELEROMETER_ROTATION to 0
D/OrientationControl: ACCELEROMETER_ROTATION putInt result: true
D/OrientationControl: Setting USER_ROTATION to 0 (Portrait)
D/OrientationControl: USER_ROTATION putInt result: true
I/OrientationControl: Successfully set orientation to: Portrait
```

## License

Copyright (c) 2026 AWare

## Contributing

Contributions welcome! Please ensure:
- Functional programming principles
- Immutable state management
- Comprehensive unit tests
- Either-based error handling
- Minimal UI design
