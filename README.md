# Minimal

Text-only Android launcher — black screen, white type, no icons. Focus sessions, app limits, offline usage insights. Everything stays on your device.

[github.com/innovatorved/minimal](https://github.com/innovatorved/minimal)

## Quick start

1. Install the APK and set **Minimal** as your default home app.
2. Grant **usage access** (Settings → usage access) for screen time and insights.
3. Optional: enable **device admin** for double-tap lock.
4. Optional (ADB): `adb shell pm grant com.minimalist.launcher android.permission.WRITE_SECURE_SETTINGS` for system-wide dark schema.

## Build

```bash
./gradlew assembleDebug    # development
./gradlew assembleRelease  # production
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/release/app-release.apk
```

**Package:** `com.minimalist.launcher`
