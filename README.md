# Offline Mesh Communication (OMC) — Android Application

> Chat without internet, towers, or routers. Every phone becomes a node in a
> self-healing mesh: discover peers over BLE, connect over Wi-Fi Direct, relay
> messages hop-by-hop, survive disconnections with store-and-forward, and confirm
> delivery with ACKs.

## Table of Contents

- [Why OMC](#why-omc)
- [How It Works](#how-it-works)
- [Features](#features)
- [Project Status](#project-status)
- [Tech Stack](#tech-stack)
- [Repository Structure](#repository-structure)
- [Requirements](#requirements)
- [Build & Run](#build--run)
- [Running the Tests](#running-the-tests)
- [Permissions (Planned)](#permissions-planned)
- [Documentation Index](#documentation-index)
- [Key Contracts](#key-contracts-do-not-drift-without-a-change-request)
- [Roadmap](#roadmap)
- [Contributing](#contributing)
- [Submission Checklist](#submission-checklist)
- [License](#license)

## Why OMC

Centralized mobile networks fail exactly when communication matters most:

| Scenario | What breaks | What OMC does instead |
|---|---|---|
| Floods / earthquakes | Towers destroyed, fiber cut | Phones talk directly, relay via each other |
| Stadiums / festivals / protests | Cells congested or shut down | Local mesh absorbs the load |
| Trekking / mining / rural areas | No coverage at all | Off-grid group messaging |
| Field / tactical ops | No trusted infrastructure | Zero-server, software-only link |

No SIM, no data pack, no root, no extra hardware — just the phones people already carry.

## How It Works

```
 ┌──────────┐   BLE advertise/scan   ┌──────────┐   Wi-Fi Direct   ┌──────────┐
 │  Node A  │ ◄────────────────────► │  Node B  │ ◄──────────────► │  Node C  │
 │ (sender) │       discovery        │ (relay)  │     transfer     │  (dest)  │
 └──────────┘                        └──────────┘                  └──────────┘
       │                                   │                             │
       │   DATA (ttl=10) ──► forward ─────►│──► forward (ttl=9) ──► deliver│
       │ ◄─────────────── ACK ─────────────┴─────────────────────────────┘
       │   (no path? STORE locally → auto-forward when a route appears)  │
```

Pipeline: **Discover → Connect (+HELLO) → Route (flooding + TTL + dedup) →
Store if unreachable (DTN) → ACK on delivery → Heartbeat/self-heal**,
all kept alive by a foreground service. Full protocol spec: OMC/1.0 in
[`docs/04-SDD-Software-Design-Document.md`](docs/04-SDD-Software-Design-Document.md).

## Features

Planned for v1 (see SRS [`docs/03`](docs/03-SRS-Software-Requirements-Specification.md)):

- 🔍 **Infrastructure-free discovery** — BLE/Nearby advertising + scanning, first peer in ≤ 10 s
- 🔗 **One-tap P2P links** — auto-accept, HELLO handshake, neighbor table
- 💬 **1-to-1 + broadcast text** (up to 4 KB) with ⏳ Pending / ➤ Sent / ✓ Delivered / ✗ Failed states
- 🌐 **Multi-hop relay** — flooding with TTL, duplicate suppression, split-horizon loop safety
- 📦 **Delay-tolerant store-and-forward** — messages wait out disconnections, deliver on reconnect, expire sanely
- ✅ **Delivery ACKs** — bounded retries with backoff
- 💓 **Self-healing** — 4 s heartbeats, dead-node detection ≤ 12 s, automatic re-routing
- 🔋 **Battery-conscious** — adaptive scan intervals, ≤ 8%/hr active budget
- 🩺 **Diagnostics screen** — node ID, peers, routing table, pending queue, drop counters

## Project Status

| Area | State |
|---|---|
| Academic docs (proposal → dissertation) | ✅ Complete in [`docs/`](docs/) |
| Android Java boilerplate (`com.example.omc`, minSdk 26) | ✅ Complete |
| Mesh implementation (discovery → ACK → DTN → Service) | ✅ Complete (Sprints 1–4 fully implemented) |

## Tech Stack

| Layer | Choice |
|---|---|
| Language | Java 17 |
| UI | XML layouts + AppCompat / Material |
| Discovery + transfer | Google Nearby Connections API (`P2P_CLUSTER`) |
| Serialization | JSON (Gson) |
| Storage | Room (SQLite) |
| Concurrency | Executors / Handlers (service-scoped, no leaks) |
| Background | Foreground service + `BOOT_COMPLETED` receiver (opt-in) |
| Build | Gradle 8.7 + Android Gradle Plugin 8.5.2 |
| Tests | JUnit4 (unit), Espresso + AndroidX Test (instrumented) |

## Repository Structure

```
.
├── app/
│   ├── build.gradle                        # module deps + SDK levels
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml         # launcher activity (permissions added as features land)
│       │   ├── java/com/example/omc/
│       │   │   └── MainActivity.java       # placeholder activity
│       │   └── res/
│       │       ├── layout/activity_main.xml
│       │       └── values/{strings,colors,themes}.xml
│       ├── test/java/com/example/omc/      # JVM unit tests
│       └── androidTest/java/com/example/omc/ # on-device tests
├── docs/                                   # academic pack (Docs 01–07)
│   ├── 01-Project-Proposal-Synopsis.md
│   ├── 02-RPD-Requirements-Planning-Document.md
│   ├── 03-SRS-Software-Requirements-Specification.md
│   ├── 04-SDD-Software-Design-Document.md
│   ├── 05-Test-Plan-and-Test-Cases.md
│   ├── 06-Deployment-and-User-Manual.md
│   └── 07-Final-Project-Report-Dissertation.md
├── gradle/wrapper/gradle-wrapper.properties
├── build.gradle / settings.gradle / gradle.properties
└── README.md (this file)
```

Intended package map as features land (mirrors SDD §2.2):
`mesh/` · `discovery/` · `connection/` · `protocol/` · `routing/` ·
`dtn/` · `ack/` · `heartbeat/` · `security/` · `storage/` · `service/` · `ui/`

## Requirements

- **Android Studio** Hedgehog or newer + **JDK 17**
- **Android SDK 34** (`minSdk 26`, `targetSdk 34`)
- A physical Android phone (API 26+) for mesh testing — emulators cover UI only
- Google Play Services on test devices (Nearby Connections dependency)

## Build & Run

Open this folder in Android Studio and let it sync, then **Run ▶** on a device.
CLI (once the wrapper jar exists — run `gradle wrapper` a single time if needed):

```bash
./gradlew assembleDebug
./gradlew installDebug
adb shell am start -n com.example.omc/.MainActivity
```

Quick smoke test (needs 2 phones, airplane mode ON → Bluetooth ON):

1. Install on both, grant all runtime permissions, tap **Start Mesh** (once implemented).
2. Each phone should list the other in **Peers** within ~10 s.
3. Send a chat message → ✓ Delivered.

Full steps: [`docs/06-Deployment-and-User-Manual.md`](docs/06-Deployment-and-User-Manual.md)

## Running the Tests

```bash
./gradlew test                    # JVM unit tests
./gradlew connectedAndroidTest    # on-device tests (device/emulator attached)
```

Quality gates for v1: ≥ 70% coverage on core modules, 100% of Critical+High
cases in [`docs/05-Test-Plan-and-Test-Cases.md`](docs/05-Test-Plan-and-Test-Cases.md).

During field runs, capture mesh logs with:

```bash
adb logcat -s OMC-MESH OMC-ROUTE OMC-DTN OMC-ACK OMC-HB > run1.log
```

## Permissions (Planned)

Declared in the manifest as the modules that need them land:

| Permission | Why |
|---|---|
| `BLUETOOTH_SCAN / ADVERTISE / CONNECT` | Discovery + transfer |
| `ACCESS_FINE_LOCATION` | Required by Android for BLE scans (no location is stored or sent) |
| `NEARBY_WIFI_DEVICES` | Wi-Fi Direct transport (API 33+) |
| `FOREGROUND_SERVICE` + `POST_NOTIFICATIONS` | Persistent mesh service |
| `RECEIVE_BOOT_COMPLETED` | Optional auto-start on boot |

## Documentation Index

Read in numeric order — each document feeds the next.

| # | Document | File | Purpose | Audience |
|---|---|---|---|---|
| 01 | Project Proposal / Synopsis | [`docs/01-Project-Proposal-Synopsis.md`](docs/01-Project-Proposal-Synopsis.md) | Scope, objectives, audience, feasibility for approval | Committee, Guide |
| 02 | Requirements Planning Document (RPD) | [`docs/02-RPD-Requirements-Planning-Document.md`](docs/02-RPD-Requirements-Planning-Document.md) | Methodology, WBS, 16-week schedule + Gantt, resources, risks, quality | Team, Guide |
| 03 | Software Requirements Specification (SRS) | [`docs/03-SRS-Software-Requirements-Specification.md`](docs/03-SRS-Software-Requirements-Specification.md) | Functional (FR-1–FR-10), non-functional, user stories, acceptance AC-1–AC-8 | Devs, Testers |
| 04 | Software Design Document (SDD) | [`docs/04-SDD-Software-Design-Document.md`](docs/04-SDD-Software-Design-Document.md) | Architecture, components, ER/Room schema, DFDs, OMC/1.0 protocol, APIs | Devs |
| 05 | Test Plan & Test Cases | [`docs/05-Test-Plan-and-Test-Cases.md`](docs/05-Test-Plan-and-Test-Cases.md) | Strategy, 60 test cases (TC-D/C/M/R/N/A/H/S/P/E), metrics, report template | Testers, Guide |
| 06 | Deployment & User Manual | [`docs/06-Deployment-and-User-Manual.md`](docs/06-Deployment-and-User-Manual.md) | User guide (Part A) + build/deploy/dev setup (Part B) + viva checklist | Users, Devs |
| 07 | Final Project Report / Dissertation | [`docs/07-Final-Project-Report-Dissertation.md`](docs/07-Final-Project-Report-Dissertation.md) | Master report: survey, design, implementation, results, future scope | Committee, Examiners |

## Key Contracts (Do Not Drift Without a Change Request)

- **Protocol:** OMC/1.0 frozen at Doc 04 §6 (Week 4 gate per RPD §10).
- **Acceptance:** SRS §8 AC-1–AC-8 — the viva pass/fail bar.
- **Quality gates:** core coverage ≥ 70%, Critical+High tests 100% (Doc 05 §3.2).

## Roadmap

- [x] Docs 01–07 + Java boilerplate
- [x] Discovery + connection (Sprint 1) → 2-phone visibility
- [x] 1-hop chat (Sprint 2) → offline texting + mid review
- [x] Flooding multi-hop + DTN store-and-forward (Sprint 3)
- [x] ACK + heartbeat/self-heal + foreground service (Sprint 4)
- [x] UI screens (Home, Chat, Peers, Logs, Settings) + SQLite storage
- [ ] Field testing, metrics, dissertation print, viva demo (Wks 13–16)
- Future (v2): AODV routing · end-to-end encryption · iOS bridge · media/voice notes · LoRa gateway

## Contributing

1. Branch from `main`: `feature/<short-name>` (e.g. `feature/discovery`).
2. One PR per backlog item — link the FR/Test ID, list devices tested, attach a log excerpt.
3. Every PR must keep `./gradlew test` green and add/extend unit tests for `protocol/`, `routing/`, `dtn/`.
4. Never change the OMC/1.0 packet format without a written change request (RPD §10).


## Contributors
| | |
|---|---|
| **Veer Shah** | [GitHub](https://github.com/veershah696) |
| **Sayal Shah** | [GitHub](https://github.com/Sayal-EV) |
| **Dhanashri Adawade** | [GitHub](https://github.com/Dhanashri-Adawade) |
| **Your Name** | [GitHub](put your github link) |

## Submission Checklist

- [ ] Fill all `____________` blanks (names, dates, device matrix, measured results).
- [ ] Redraw RPD Gantt in GanttProject/Excel for the printed copy.
- [ ] Attach screenshots (Doc 07 App. D) and signed test cycles (Doc 05 §9).
- [ ] Render Doc 07 into the institutional template before printing.

## License

TBD — pick one before publishing (e.g. MIT for code; CC-BY-4.0 suits the docs).
If this is a university submission, confirm with your guide whether the
institution requires a specific license or an IP declaration first.
