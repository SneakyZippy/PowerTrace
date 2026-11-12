# Google Play Store Listing & Compliance Reference

## 1. Store Listing Copy

### App Title (28 / 30 chars)
```
PowerLab: Battery & Charging
```

### Short Description (79 / 80 chars)
```
Benchmark chargers, isolate wakelock drain, and monitor battery health offline.
```

### Full Description (< 4,000 chars)
```
PowerLab is a privacy-first Android battery diagnostics, wakelock isolation, and charging hardware profiler. Designed for both sides of your device's power lifecycle, it gives you lab-grade visibility into how power enters and leaves your phone—with zero tracking and zero network permissions.

CHARGING TELEMETRY (POWER IN)
• Live Wattage & Current: Measure real-time charging watts, net current (mA), voltage, and battery temperature curves.
• Charger & Cable Benchmarking: Catalog your USB-C cables, power bricks, and wireless pads to compare real-world charging speeds and spot thermal throttling or bottlenecked cables.
• Session History & Charts: Scrub through interactive charging curves with battery percentage overlays and peak/average power summaries.

DRAIN DIAGNOSTICS (POWER OUT)
• Wakelock & Wakeup Isolation: Pinpoint apps and processes triggering partial wakelocks, kernel wakeups, alarms, and background CPU spikes while your screen is off.
• 24/7 Continuous Telemetry: Optional background monitoring records periodic snapshots to calculate deep sleep vs. awake ratios across the day.
• Observation Sessions: Run dedicated standby or overnight benchmarks with before/after differential snapshot subtraction.
• Exportable Reports: Generate and share HTML and CSV diagnostic reports locally via Android's share sheet.

MOCK DEMO MODE INCLUDED
Want to explore the interface before granting diagnostic permissions? Toggle Mock Diagnostic Mode in Settings or tap Proceed (Demo) to test every screen with simulated telemetry.

PERMISSIONS & PRIVACY
PowerLab contains NO internet permission (android.permission.INTERNET), NO ads, NO analytics SDKs, and NO cloud servers. 100% of your telemetry stays on your device in a private local database.

• Foreground Service (Special Use): Used only when you enable 24/7 Continuous Telemetry or start a manual Observation Session, displaying a persistent status notification while recording live battery metrics.
• Usage Access (PACKAGE_USAGE_STATS — optional): Used solely to correlate background app activity with screen-off battery drain intervals. Can be granted via Android Settings or ADB.
• Elevated Diagnostic Stats (BATTERY_STATS, DUMP, INTERACT_ACROSS_USERS — optional via ADB): Standard Android diagnostic permissions that allow non-root differential snapshots of local battery and alarm summaries.

Privacy Policy: https://sneakyzippy.github.io/privacy-policy/
```

---

## 2. Play Console Declarations

### Foreground Service (`FOREGROUND_SERVICE_SPECIAL_USE`)
* **Subtypes in Manifest:** `continuous_battery_diagnostics` (`ContinuousMonitoringService`), `battery_monitoring_session` (`BatterySessionService`)
* **User-facing task:** Displays an ongoing status-bar notification showing live battery/charging monitoring state and session progress.
* **Why alternatives (`WorkManager` / `AlarmManager`) fail:** Real-time charging wattage/current sampling, screen-off/screen-on transition capture, and continuous user-controlled observation sessions require an active foreground service so the OS does not defer or kill active telemetry.

### Usage Access (`PACKAGE_USAGE_STATS`)
* Optional permission used exclusively on-device to correlate foreground/background app activity with screen-off battery drain intervals. No usage data ever leaves the device.

### Data Safety
* **Does your app collect or share any of the required user data types?** **No** (no `INTERNET` permission, no third-party SDKs, 100% local Room SQLite storage).

---

## 3. Asset Checklist
* **App Icon (`512×512`):** `design/powerlab_playstore_icon_512.png`
* **Feature Graphic (`1024×500`):** `design/powerlab_feature_graphic_1024x500.png`
* **Screenshots:** `docs/screenshots/` (`overview_telemetry.png`, `overview_culprits.png`, `analytics_insights.png`)
* **Privacy Policy URL:** `https://sneakyzippy.github.io/privacy-policy/` (verify HTTP 200 and PowerLab branding before submitting)
