# Document 06 — Deployment & User Manual

## Operational Guide: Install, Configure, Build, Run, and Troubleshoot OMC

| Field | Detail |
|---|---|
| **Document ID** | OMC-DEPLOY-1.0 |
| **Version** | 1.0 |
| **Date** | September 2026 |
| **Audience** | Developers, testers, end users, evaluators |
| **Applies To** | APK `v1.0` / source at tag `v1.0-final` |

---

## PART A — FOR END USERS (Non-Technical)

### A.1 What OMC Does (30-Second Version)

OMC lets nearby Android phones chat **without internet, signal, or Wi-Fi router**. Open the app, tap **Start Mesh**, pick a person, type, and send. If the person is not in direct range, other OMC phones relay the message automatically. If nobody can reach them right now, the message waits and delivers itself later.

### A.2 What You Need

- Android phone, version 8.0 or newer.
- Bluetooth turned ON.
- Location permission granted (Android requires this for nearby scanning — OMC does not track you).
- 50 MB free space.
- A second Android phone with OMC nearby for testing.

> No SIM, no data pack, no Wi-Fi needed. Airplane mode + Bluetooth ON is the ideal demo state.

### A.3 Installation (APK Sideload)

1. Copy `omc-v1.0.apk` to the phone (USB, Bluetooth, or Drive).
2. Tap the APK → allow **Install from unknown sources** when asked (one-time).
3. Tap **Install** → **Open**.
4. (Existing Play version, if published: search "OMC Mesh" → Install — same steps after.)

### A.4 First-Time Setup (≤ 2 Screens)

1. **Welcome → Continue.** Read the offline-first note.
2. **Permissions screen → Grant all:** Nearby devices, Location, Bluetooth, Notifications. Without these the mesh cannot see peers.
3. **Display name:** enter e.g. `Dinesh-Pixel` → **Save**.
4. **Battery tip card:** tap **Disable optimization** → select OMC → **Don't optimize**. This keeps the mesh alive in background (critical on Xiaomi/Oppo/Vivo).
5. Tap **Start Mesh**. The persistent notification `OMC Mesh running · 0 peers` confirms it.

### A.5 Discovering Peers

1. Go to **Peers** tab.
2. Nearby OMC users appear within ~10 seconds with a green dot (connected) or grey dot (visible, tap to connect).
3. If nobody appears: confirm the other phone also tapped **Start Mesh**, Bluetooth is ON, and distance is under ~20 m.

### A.6 Sending a Message (≤ 3 Taps)

1. **Chat** tab → tap a peer (or **Broadcast** for everyone nearby).
2. Type (max ~4000 characters) → tap **Send**.
3. Watch the tick:
   - ⏳ **Pending** — waiting for a path (will auto-send later).
   - ➤ **Sent** — handed to the mesh.
   - ✓ **Delivered** — destination confirmed (ACK received).
   - ✗ **Failed** — retries exhausted; long-press → **Retry**.

### A.7 Receiving Messages

Nothing to do — incoming chats pop into the same **Chat** list with sender name + time. Relayed messages may show `via 2 hops` (informational).

### A.8 Broadcast Alerts

**Chat → Broadcast → type → Send.** Reaches every connected peer (each sees it once). Use for "Evacuation at Gate 3" style announcements. Broadcasts do not request delivery receipts by default.

### A.9 Store-and-Forward (Delayed Delivery)

If the recipient is offline/out of range, your message stays **Pending** — do NOT delete it. When their phone reappears, delivery is automatic. Check **Diagnostics → Pending** to see the queue.

### A.10 Settings That Matter

| Setting | Default | When to Change |
|---|---|---|
| Display name | Device model | Set a human name before a group drill |
| TTL (max hops) | 10 | Lower (3–5) in a dense room to reduce chatter |
| Heartbeat | 4 s | Leave alone unless guide instructs |
| Message expiry | 24 h | Shorten (1 h) for drills to keep queue clean |
| Auto-start on boot | Off | Turn ON for responder phones |
| Sign messages | Off | Turn ON only if all test phones enable it |

### A.11 Battery Tips

- Leave the foreground notification alone — swiping it away can stop the mesh on some phones.
- Exempt OMC from battery optimization (§A.4 step 4).
- Lower TTL and stop the mesh when done (**Home → Stop Mesh**).

### A.12 Troubleshooting (User Table)

| Symptom | Fix (in order) |
|---|---|
| No peers found | BT ON? Both apps on Start Mesh? Permissions granted? Move < 20 m; restart mesh |
| Stuck on Pending | Peer offline? Wait for reconnect or move closer; check Diagnostics pending count |
| Messages slow | Congested RF — retry; reduce TTL; fewer concurrent senders |
| App stops in background | Disable battery optimization; lock app in Recents (OEM "lock" icon) |
| Permission denied loop | Settings → Apps → OMC → Permissions → Allow all → restart mesh |
| Notification gone | Mesh stopped — reopen app → Start Mesh |

### A.13 Uninstall

Uninstall like any app (long-press → Uninstall). Local chat history is deleted with the app — export anything needed first (Diagnostics → Share log does not export chats; screenshot instead).

---

## PART B — FOR DEVELOPERS AND TESTERS

### B.1 Prerequisites

- Android Studio Hedgehog or newer, JDK 17 (`java -version` → 17).
- Android SDK 34, Gradle 8+ (wrapper included — no manual install).
- Git; `adb` on PATH (ships with Studio platform-tools).
- A GMS device for Nearby Connections (emulator OK for UI, **physical phones required** for mesh).

### B.2 Clone and Configure

```bash
git clone https://github.com/your-org/omc-android.git
cd omc-android
git checkout v1.0-final   # or develop for latest
```

Create `local.properties` (never commit):

```properties
sdk.dir=C\:\\Users\\YOURNAME\\AppData\\Local\\Android\\Sdk
```

Confirm `app/build.gradle.kts`: `minSdk 26`, `targetSdk 34`, `versionName "1.0"`.

### B.3 Permissions and Manifest Checklist (pre-build)

- `BLUETOOTH_SCAN / BLUETOOTH_ADVERTISE / BLUETOOTH_CONNECT` (with `neverForLocation` where applicable).
- `ACCESS_FINE_LOCATION` (needed ≤ Android 11 for BLE scan; request at runtime).
- `NEARBY_WIFI_DEVICES` (Android 13+).
- `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_CONNECTED_DEVICE`.
- `POST_NOTIFICATIONS` (Android 13+), `RECEIVE_BOOT_COMPLETED` (behind setting).
- Service declared: `MeshForegroundService` with `foregroundServiceType="connectedDevice"` + exported receiver for boot.

### B.4 Build

```bash
./gradlew assembleDebug          # dev APK
./gradlew assembleRelease        # needs signing key (below)
./gradlew lint test              # static analysis + unit tests
```

APK outputs: `app/build/outputs/apk/debug/app-debug.apk`.

### B.5 Install and Run

```bash
adb devices                      # confirm device
./gradlew installDebug
adb shell am start -n com.example.omc/.MainActivity
```

Or manual: `adb install -r app/build/outputs/apk/debug/app-debug.apk`.

**Two-phone smoke test (5 min):**

1. Both phones: airplane mode ON → BT ON → install → grant permissions → Start Mesh.
2. Each should list the other ≤ 10 s (TC-D-01).
3. A→B "hello" → B displays; A shows ✓ (TC-M-01, TC-A-01).
4. Enable third phone between them, move A/C apart → A→C via B (TC-R-01).

### B.6 Run Tests

```bash
./gradlew test                   # JVM unit tests (all modules)
./gradlew testDebugUnitTest --tests "com.example.omc.routing.*"
./gradlew connectedAndroidTest   # instrumented (device/emulator attached)
./gradlew jacocoTestReport       # coverage; gate: core >= 70%
```

Log capture during field runs:

```bash
adb logcat -s OMC-MESH OMC-ROUTE OMC-DTN OMC-ACK OMC-HB > run1.log
```

### B.7 Project Structure (Map to SDD §2.2)

```
omc-android/
  app/src/main/java/com/example/omc/
    mesh/ discovery/ connection/ protocol/
    routing/ dtn/ ack/ heartbeat/ security/
    storage/ service/ ui/
  app/src/test/                  # unit
  app/src/androidTest/           # instrumented
  docs/                          # Docs 01-07 (this repo)
  .github/workflows/ci.yml       # assemble + test + lint
```

### B.8 Signing a Release

1. `Build → Generate Signed Bundle/APK` → new keystore (back it up; losing it = new app identity).
2. Set `versionCode` (+1 per release), `versionName "1.0"`.
3. `./gradlew assembleRelease` / `bundleRelease`; verify with `apksigner verify --print-certs`.
4. Record SHA-256 in release notes (§B.10).

### B.9 Deployment Checklist (pre-viva / pre-release)

- [ ] `versionCode/Name` set; changelog written
- [ ] R8/ProGuard passes; release APK smoke-tested on 2 phones
- [ ] All permissions justified in-app (no "optional" permission blocks core flow silently)
- [ ] Foreground notification shows peer + pending counts; Stop action works
- [ ] Diagnostics counters reset per session; log export works
- [ ] Crashlytics (if integrated) DSYM/mapping uploaded
- [ ] `docs/` rebuilt to match final behavior; build hash recorded in Test Report §9
- [ ] Demo devices charged, APKs pre-installed, airplane-mode drill rehearsed

### B.10 Release Notes Template

```
Version: 1.0.0 (versionCode 10)   Date: YYYY-MM-DD   Build: <sha>
Requires: Android 8.0+  Size: __MB  SHA-256: __
New: multi-hop flooding, DTN store-forward, ACK receipts, heartbeat self-heal
Fixed: ...   Known issues: ... (e.g., GMS required; AOSP-only unsupported)
Tested on: <models + APIs>
```

---

## PART C — OPERATOR / DEMO CHECKLIST (Viva Day)

1. Charge all phones ≥ 80%; pre-install same APK; set distinct display names.
2. Print one-page topology card (e.g., "A–B–C line, A/C separated").
3. Rehearse: discovery → 1-hop → multi-hop → kill-B → pending → rejoin → delivered → diagnostics tour.
4. Keep one spare phone + power bank + USB cables on the table.
5. Record the demo (screen + wide shot) as backup if live RF misbehaves.

*Next document: 07 — Final Project Report / Dissertation (master academic document).*
