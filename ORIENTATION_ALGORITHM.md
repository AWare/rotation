# Multi-Screen Orientation Tracking Algorithm

## Overview
This document describes how the app tracks and applies orientation settings across multiple screens independently.

## Data Structures

```kotlin
// Track what orientation is currently applied to each screen
private val appliedOrientations = MutableStateFlow<Map<Int, AppliedOrientationState>>(emptyMap())

data class AppliedOrientationState(
    val packageName: String,           // Which app caused this orientation
    val orientation: ScreenOrientation, // What orientation is applied
    val targetScreen: TargetScreen     // Which screen it targets
)
```

## Algorithm Flow Diagram

```
┌─────────────────────────────────────────────────────────────────────┐
│                    ACCESSIBILITY EVENT RECEIVED                      │
│                    (App Switch Detected)                             │
└────────────────────────────┬────────────────────────────────────────┘
                             │
                             ▼
                    ┌────────────────────┐
                    │ Extract Package    │
                    │ Name from Event    │
                    └─────────┬──────────┘
                              │
                              ▼
                    ┌────────────────────┐
                    │ Debounce Check     │
                    │ (150ms window)     │
                    └─────────┬──────────┘
                              │
                              ▼
                    ┌────────────────────┐
                    │ Same Package?      │
                    └─────────┬──────────┘
                              │
                    ┌─────────┴─────────┐
                    │ Yes               │ No
                    ▼                   ▼
             ┌──────────┐      ┌────────────────────┐
             │ Ignore   │      │ handleAppSwitch()  │
             └──────────┘      └─────────┬──────────┘
                                         │
                 ┌───────────────────────┴───────────────────────┐
                 │                                               │
                 ▼                                               ▼
    ┌────────────────────────┐                    ┌─────────────────────────┐
    │ 1. RESET PHASE         │                    │ 2. APPLY PHASE          │
    │                        │                    │                         │
    │ For previous app:      │                    │ For new app:            │
    │                        │                    │                         │
    │ a) Get all screens     │                    │ a) Check if launcher?   │
    │    where orientations  │                    │    ├─ Yes: Apply global │
    │    were applied        │                    │    │   to all screens   │
    │                        │                    │    └─ No: Continue       │
    │ b) For each screen:    │                    │                         │
    │    - Get global        │                    │ b) Get app's settings   │
    │      orientation       │                    │    for each screen      │
    │    - Apply to screen   │                    │                         │
    │    - Remove from       │                    │ c) For each setting:    │
    │      tracking          │                    │    - Apply orientation  │
    │                        │                    │      to target screen   │
    │ c) Clear all tracked   │                    │    - Add to tracking    │
    │    orientations for    │                    │      map                │
    │    previous app        │                    │                         │
    └────────────────────────┘                    └─────────────────────────┘
```

## Multi-Screen Tracking

### Per-Screen State
Each physical display is tracked independently:

```
Display 0 (Built-in):  appliedOrientations[0] = {
                         packageName: "com.example.app",
                         orientation: Portrait,
                         targetScreen: TargetScreen(id=0)
                       }

Display 1 (External):  appliedOrientations[1] = {
                         packageName: "com.example.app",
                         orientation: Landscape,
                         targetScreen: TargetScreen(id=1)
                       }
```

### Example Flow

#### Scenario: User has Chrome set to Portrait on Display 0, Landscape on Display 1

```
Step 1: User opens Chrome
├─ Detect: packageName = "com.chrome.android"
├─ Reset Phase: No previous app, skip
└─ Apply Phase:
   ├─ Not launcher ✓
   ├─ Get settings for Chrome:
   │  ├─ Setting 1: Portrait, Display 0
   │  └─ Setting 2: Landscape, Display 1
   ├─ Apply Portrait → Display 0
   ├─ Apply Landscape → Display 1
   └─ Track: appliedOrientations = {
        0: {chrome, Portrait, Display0},
        1: {chrome, Landscape, Display1}
      }

Step 2: User closes Chrome (goes to launcher)
├─ Detect: packageName = "com.android.launcher3"
├─ Reset Phase:
│  ├─ Previous app: Chrome
│  ├─ Get tracked screens: [0, 1]
│  ├─ Get global orientation: Auto
│  ├─ Apply Auto → Display 0
│  ├─ Apply Auto → Display 1
│  └─ Clear tracking: appliedOrientations = {}
└─ Apply Phase:
   ├─ Is launcher ✓
   ├─ Apply global (Auto) → All displays
   └─ No tracking (launcher uses global)
```

## Key Principles

1. **Always Reset When Leaving**: Every time we leave an app, reset ALL screens that had custom orientations applied

2. **Independent Screen Tracking**: Each display maintains its own orientation state independently

3. **Global Orientation Fallback**: When no custom orientation is set (launcher, unknown apps), use global preference

4. **State Synchronization**: Track what we applied so we know exactly what to reset

5. **Atomic Operations**: State transitions are synchronized to prevent race conditions

## Launcher Detection

Multiple methods to detect launcher apps:

1. **Primary**: Query apps with HOME intent + CATEGORY_HOME
2. **Fallback**: Check if package name contains "launcher"
3. **System**: Check if package name is system launcher (com.android.launcher3)

## Error Handling

- If display is disconnected while orientation applied → Clear from tracking
- If app uninstalled while orientation applied → Clear from tracking on next access
- If service crashes → State is rebuilt on service restart
