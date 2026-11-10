# Privacy Policy for PowerTrace

**Last Updated:** November 2026

**Live Policy URL:** [https://sneakyzippy.github.io/privacy-policy/](https://sneakyzippy.github.io/privacy-policy/)

PowerTrace is built with a strict privacy-first architecture. This application is designed to be completely transparent, secure, and private.

---

### 1. Zero Network Transmission & Offline By Design
* PowerTrace does not include internet permissions (`android.permission.INTERNET`) in its manifest.
* The application does not contain advertising SDKs, tracking libraries, analytics frameworks, or crash-reporting servers.
* **100% of diagnostic data, battery readings, and session history remain on your local device.**

---

### 2. Information Collected and Processed Locally
To provide battery health and wakelock analytics, PowerTrace processes the following data locally on your device:
* **Battery Statistics:** Discharge rates, temperature, voltage, capacity, charging session duration, and power profiles.
* **Wakelocks and Alarms:** Per-app kernel/partial wakelock durations and alarm frequencies via the local Android `dumpsys` interface.
* **Package Information:** Names and icons of installed applications associated with UIDs to present human-readable diagnostic reports.

All collected metrics are stored solely in a local SQLite database (`Room`) within the app's sandboxed private storage directory.

---

### 3. Elevated Diagnostic Permissions (ADB)
PowerTrace optionally uses standard Android diagnostic permissions granted explicitly by the user via Android Debug Bridge (ADB):
* `android.permission.BATTERY_STATS`: Reads battery discharge statistics across hardware subsystems.
* `android.permission.DUMP`: Reads local system service summaries (`dumpsys alarm`, `dumpsys power`) to compute differential drain between snapshots.
* `android.permission.PACKAGE_USAGE_STATS`: Correlates foreground app activity with screen-off intervals.

These permissions are never used to access personal files, keystrokes, messages, contacts, or sensitive communications.

---

### 4. Data Sharing and Third Parties
* **Zero Data Sharing:** Because PowerTrace does not communicate with external servers, no diagnostic or personal information is ever sold, transferred, or disclosed to third parties.
* **User-Initiated Export:** You may export your diagnostic reports (HTML/CSV) using Android's native share sheet. These exports are generated strictly upon your manual request.

---

### 5. Open Source & Transparency
PowerTrace is open-source software. You are encouraged to inspect the source code, verify permissions, and review the build pipeline directly in the repository.

---

### 6. Contact
For questions regarding this privacy policy or PowerTrace diagnostics, please open an issue on the project's GitHub repository or visit [https://sneakyzippy.github.io/privacy-policy/](https://sneakyzippy.github.io/privacy-policy/).
