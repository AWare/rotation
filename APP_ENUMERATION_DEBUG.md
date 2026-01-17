# App Enumeration Debugging Guide

## Problem: Only 4 Apps Showing in List

If you're still seeing only 4 apps in the app list, follow this debugging guide.

---

## Changes Made

### 1. **AndroidManifest.xml**

Added required elements for Android 11+ app visibility:

```xml
<!-- Permission to see all packages -->
<uses-permission android:name="android.permission.QUERY_ALL_PACKAGES"
    tools:ignore="QueryAllPackagesPermission" />

<!-- Explicit declaration of what we query -->
<queries>
    <intent>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent>
</queries>
```

### 2. **MainViewModel.kt**

Updated to use proper flags that Android launchers use:

```kotlin
val queryFlags = PackageManager.MATCH_DISABLED_COMPONENTS or
                PackageManager.MATCH_DISABLED_UNTIL_USED_COMPONENTS

val resolveInfos = packageManager.queryIntentActivities(
    launcherIntent,
    PackageManager.ResolveInfoFlags.of(queryFlags.toLong())
)
```

### 3. **Added Debug Logging**

Three log points to trace the issue:
- After `queryIntentActivities()` - shows raw count
- After filtering/mapping - shows count after processing
- Final count - shows combined count with saved settings

---

## How to Debug

### Step 1: Check Logcat

Run the app and filter logcat for "MainViewModel":

```bash
adb logcat | grep MainViewModel
```

**Expected output (working correctly):**
```
D/MainViewModel: Found 50 apps from queryIntentActivities
D/MainViewModel: After filtering: 48 apps
D/MainViewModel: Final app list: 48 apps (48 installed + 0 from settings)
D/MainViewModel:   - Calculator (com.android.calculator2) installed=true
D/MainViewModel:   - Calendar (com.android.calendar) installed=true
D/MainViewModel:   - Chrome (com.android.chrome) installed=true
...
```

**Problem output (only 4 apps):**
```
D/MainViewModel: Found 4 apps from queryIntentActivities
D/MainViewModel: After filtering: 3 apps
D/MainViewModel: Final app list: 3 apps (3 installed + 0 from settings)
```

---

### Step 2: Verify App Installation

**CRITICAL:** You MUST uninstall and reinstall the app after manifest changes!

```bash
# Uninstall old version
adb uninstall app.rotatescreen

# Build and install new version
./gradlew installDebug
```

Permissions and `<queries>` elements only take effect on fresh install.

---

### Step 3: Check Android Version

Run this to check Android version:

```bash
adb shell getprop ro.build.version.sdk
```

- **API 29 (Android 10):** No restrictions, should show all apps
- **API 30+ (Android 11+):** Requires QUERY_ALL_PACKAGES + `<queries>`

---

### Step 4: Verify Permission in Manifest

Check the installed app's manifest:

```bash
adb shell dumpsys package app.rotatescreen | grep -A 5 "permission"
```

Should show:
```
requested permissions:
  android.permission.QUERY_ALL_PACKAGES
  android.permission.SYSTEM_ALERT_WINDOW
  ...
```

---

### Step 5: Check Queries Declaration

Verify `<queries>` is in the manifest:

```bash
adb pull /data/app/~~randompath~~/app.rotatescreen/base.apk
aapt dump xmltree base.apk AndroidManifest.xml | grep -A 5 "queries"
```

Should show the `<queries>` element with LAUNCHER intent.

---

## Common Issues & Solutions

### Issue 1: Still Only 4 Apps After Reinstall

**Possible causes:**
1. Manifest didn't merge correctly
2. Build cache issue
3. APK doesn't have the changes

**Solutions:**
```bash
# Clean build
./gradlew clean

# Rebuild
./gradlew assembleDebug

# Verify APK contains changes
aapt dump xmltree app/build/outputs/apk/debug/app-debug.apk AndroidManifest.xml | grep QUERY_ALL_PACKAGES
```

---

### Issue 2: Permission Not Granted

On some devices, `QUERY_ALL_PACKAGES` may require manual grant:

**Check if permission is granted:**
```bash
adb shell dumpsys package app.rotatescreen | grep QUERY_ALL_PACKAGES
```

**Should see:**
```
android.permission.QUERY_ALL_PACKAGES: granted=true
```

If `granted=false`, try:
```bash
adb shell pm grant app.rotatescreen android.permission.QUERY_ALL_PACKAGES
```

---

### Issue 3: queryIntentActivities Returns Wrong Count

**Test the query directly:**

```bash
# Get list of launcher apps
adb shell cmd package query-activities \
  --brief \
  -a android.intent.action.MAIN \
  -c android.intent.category.LAUNCHER
```

This shows what the system sees. Compare with your app's log count.

If system shows 50+ but app shows 4:
- Check flags being used
- Verify `<queries>` element
- Check for app-specific restrictions

---

### Issue 4: Specific Apps Missing

Some apps may not be visible due to:

1. **No launcher activity** - App has no main activity (services, widgets only)
2. **Hidden by manufacturer** - Some OEMs hide system apps
3. **Work profile** - Apps in work profile are separate
4. **App suspended** - App is suspended by admin/parent control

**Check specific app:**
```bash
adb shell pm list packages -f | grep <app_name>
adb shell cmd package query-activities \
  -a android.intent.action.MAIN \
  -c android.intent.category.LAUNCHER \
  <package_name>
```

---

## Test Cases

Run the unit tests to verify flag usage:

```bash
./gradlew testDebugUnitTest --tests "*queryIntentActivities uses correct flags*"
./gradlew testDebugUnitTest --tests "*includes disabled apps*"
```

Both tests should pass, verifying:
- ✅ Correct flags (MATCH_DISABLED_COMPONENTS)
- ✅ Correct intent (ACTION_MAIN + CATEGORY_LAUNCHER)
- ✅ All apps included (not just enabled ones)

---

## Expected Behavior

### On Android 10 and Earlier (API ≤ 29):
- Should see **all apps** immediately
- No QUERY_ALL_PACKAGES needed
- No `<queries>` needed

### On Android 11+ (API ≥ 30):
**Without QUERY_ALL_PACKAGES:**
- Sees only ~4-10 apps:
  - Own app
  - System apps (Settings, Phone, etc.)
  - Apps matching `<queries>`

**With QUERY_ALL_PACKAGES:**
- Sees **all apps with launcher activities**
- Typically 50-200 apps depending on device

---

## What Should Be in the List

**Included:**
✅ User apps (Chrome, Gmail, WhatsApp, etc.)
✅ System apps with launcher (Settings, Calculator, etc.)
✅ Disabled apps (if they have launcher activity)
✅ Recently installed apps
✅ Apps from other sources (sideloaded)

**Not Included:**
❌ This rotation app itself (filtered out)
❌ Apps without launcher activity (services, widgets)
❌ Hidden system apps (no launcher)
❌ Work profile apps (separate context)
❌ Instant apps (not fully installed)

---

## Debugging Checklist

- [ ] Uninstalled old version completely
- [ ] Clean build performed (`./gradlew clean`)
- [ ] Fresh install of debug APK
- [ ] Checked logcat for MainViewModel logs
- [ ] Verified QUERY_ALL_PACKAGES in manifest
- [ ] Verified `<queries>` element in manifest
- [ ] Tested on Android 11+ device
- [ ] Verified permission granted in dumpsys
- [ ] Compared with system's query-activities output
- [ ] Ran unit tests (both passed)

---

## Still Not Working?

If after all steps you still see only 4 apps:

### 1. Check Build Variants

Ensure you're testing the debug build with changes:

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 2. Check Merged Manifest

View the final merged manifest:

```bash
cat app/build/intermediates/merged_manifests/debug/AndroidManifest.xml
```

Search for `QUERY_ALL_PACKAGES` and `<queries>`.

### 3. Try Alternative Approach

If all else fails, use `getInstalledPackages()` as fallback:

```kotlin
// Fallback for debugging
val packages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    packageManager.getInstalledPackages(
        PackageManager.PackageInfoFlags.of(
            PackageManager.GET_ACTIVITIES.toLong()
        )
    )
} else {
    packageManager.getInstalledPackages(PackageManager.GET_ACTIVITIES)
}

// Then filter for apps with launcher activities
```

### 4. File an Issue

If none of the above works, please file an issue with:
- Logcat output (filtered for MainViewModel)
- Android version (`adb shell getprop ro.build.version.sdk`)
- Device manufacturer and model
- Output of `adb shell pm list packages | wc -l` (total packages)
- Output of merged manifest check
- Unit test results

---

## References

- [Android Package Visibility](https://developer.android.com/training/package-visibility)
- [Query All Packages Permission](https://developer.android.com/reference/android/Manifest.permission#QUERY_ALL_PACKAGES)
- [Queries Element](https://developer.android.com/guide/topics/manifest/queries-element)
- [PackageManager Flags](https://developer.android.com/reference/android/content/pm/PackageManager)

---

**Last Updated:** 2026-01-17
**Commit:** 16344cf
