# Rotation Control

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

**Build & Test** (on push/PR):
- Runs all unit tests
- Builds debug APK
- Runs lint checks
- Uploads artifacts

**Release** (on tag push):
- Runs full test suite
- Builds release APK
- Creates GitHub release
- Uploads signed APK (if keystore configured)

### Creating a Release

```bash
git tag v1.0.0
git push origin v1.0.0
```

GitHub Actions will automatically build and create a release.

### Signing (Optional)

Add these secrets to your GitHub repository:
- `KEYSTORE_BASE64`: Base64-encoded keystore file
- `KEY_ALIAS`: Keystore alias
- `KEY_PASSWORD`: Key password
- `KEYSTORE_PASSWORD`: Keystore password

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
./gradlew assembleDebug      # Debug build
./gradlew assembleRelease    # Release build
./gradlew test              # Run tests
./gradlew lint              # Run lint
```

## Requirements

- Android 10 (API 29) or higher
- Device with orientation sensors

## License

Copyright (c) 2026 AWare

## Contributing

Contributions welcome! Please ensure:
- Functional programming principles
- Immutable state management
- Comprehensive unit tests
- Either-based error handling
- Minimal UI design
