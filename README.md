# Rotation Control

An Android app for controlling screen orientation using functional programming style with Kotlin and Arrow.

## Features

- **Quick Settings Tile**: Control screen orientation directly from the Quick Settings panel
- **In-App Control**: Full-featured app interface for managing orientation settings
- **Per-App Settings**: Set different orientations for individual apps with automatic switching
- **Multi-Screen Support**: Target specific displays on multi-screen devices
- **Persistent Storage**: Settings are saved and automatically restored
- **Functional Programming**: Built using FP principles with Arrow library for type-safe error handling

## Supported Orientations

- Auto Rotate (Unspecified)
- Portrait
- Landscape
- Reverse Portrait
- Reverse Landscape
- Sensor (all orientations)

## Architecture

The app is built using modern Android development practices and functional programming:

### Tech Stack

- **Language**: Kotlin 1.9.21
- **UI**: Jetpack Compose with Material 3
- **FP Library**: Arrow 1.2.1 for functional programming (Either, Option, etc.)
- **Database**: Room 2.6.1 for persistent storage
- **Preferences**: DataStore for app preferences
- **Architecture**: MVVM with StateFlow for reactive state management

### Project Structure

```
app/src/main/java/com/aware/rotation/
├── domain/
│   └── model/              # Domain models using FP style
│       ├── ScreenOrientation.kt
│       ├── TargetScreen.kt
│       ├── AppOrientationSetting.kt
│       └── OrientationState.kt
├── data/
│   ├── local/
│   │   ├── entity/         # Room entities
│   │   ├── dao/            # Data access objects
│   │   └── RotationDatabase.kt
│   ├── repository/         # Repository pattern with Either for error handling
│   └── preferences/        # DataStore preferences
├── service/
│   ├── OrientationControlService.kt      # Handles orientation changes
│   └── ForegroundAppDetectorService.kt   # Accessibility service
├── tile/
│   └── OrientationTileService.kt         # Quick Settings tile
├── ui/
│   ├── MainActivity.kt
│   ├── MainViewModel.kt
│   ├── screen/             # Compose screens
│   ├── components/         # Reusable Compose components
│   └── theme/              # Material 3 theme
└── util/                   # Utility functions and helpers
```

## Functional Programming Patterns

This app demonstrates FP concepts:

- **Immutable Data Structures**: All domain models are immutable data classes
- **Pure Functions**: State transformations through copy functions (e.g., `withOrientation()`)
- **Either Type**: Type-safe error handling without exceptions
- **Flow**: Reactive data streams for state management
- **Composition**: Small, composable functions instead of large classes

### Example: Error Handling with Arrow

```kotlin
fun setOrientation(orientation: ScreenOrientation): Either<OrientationError, Unit> = either {
    PermissionChecker.checkWriteSettingsPermission(context).bind()
    applyOrientation(orientation).bind()
}
```

## Setup & Permissions

### Required Permissions

1. **WRITE_SETTINGS**: Required to modify system orientation settings
   - The app will prompt to grant this permission on first launch

2. **Accessibility Service**: Required for per-app orientation control
   - Detects which app is in the foreground
   - No data is collected or transmitted
   - Settings → Accessibility → Rotation Control

### Installation

1. Clone the repository
2. Open in Android Studio
3. Build and run on a device (API 29+)
4. Grant required permissions when prompted

## Usage

### Quick Settings Tile

1. Add the "Screen Rotation" tile to Quick Settings
2. Tap to cycle through orientations:
   - Auto Rotate → Portrait → Landscape → Reverse Portrait → Reverse Landscape → Sensor

### In-App Control

1. **Global Orientation**: Set a default orientation for all apps
2. **Per-App Settings**:
   - Enable the Accessibility Service
   - Search for an app
   - Select desired orientation
   - Settings are automatically applied when the app is opened

## Multi-Screen Support

The app supports targeting specific displays on devices with multiple screens:

- **All Screens**: Apply orientation to all displays
- **Primary Screen**: Target the main display
- **Secondary Screen**: Target external displays

Note: Per-display orientation control requires system-level access and may be limited on some devices.

## Database Schema

### AppOrientationEntity

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
```

## Minimum Requirements

- Android 10 (API 29) or higher
- Device with orientation sensors

## License

Copyright (c) 2026 AWare

## Contributing

Contributions are welcome! Please ensure code follows:
- Kotlin coding conventions
- Functional programming principles
- Compose best practices
- Immutable state management
