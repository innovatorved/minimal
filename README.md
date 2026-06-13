# Minimal

A distraction-free Android launcher. Black screen, white text, no icons.

## What it does

- Home screen with clock, widgets, and pinned apps
- Focus sessions and daily app limits
- Offline usage insights (data stays on your phone)
- Message alerts from saved contacts only

## Setup

1. Install the app and set **Minimal** as your default launcher.
2. In app Settings, enable **usage access** for screen time and insights.
3. Optional: enable **notification listener** and **contacts** for home message alerts.

## ADB (optional)

Some permissions cannot be toggled in the app UI. Grant them over USB debugging:

```bash
PKG=com.minimalist.launcher

# System-wide dark mode and grayscale
adb shell pm grant $PKG android.permission.WRITE_SECURE_SETTINGS

# Session alerts and calendar widget
adb shell pm grant $PKG android.permission.POST_NOTIFICATIONS
adb shell pm grant $PKG android.permission.READ_CALENDAR

# Double-tap lock (device admin)
adb shell dpm set-active-admin $PKG/com.example.deviceadmin.LauncherDeviceAdminReceiver
```

**Usage access** still must be enabled manually: Settings → Apps → Special app access → Usage access → **Minimal**.


```bash
./gradlew assembleRelease
adb install -r app/build/outputs/apk/release/app-release.apk
```

Package: `com.minimalist.launcher`
