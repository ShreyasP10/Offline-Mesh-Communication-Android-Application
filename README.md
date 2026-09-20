# Offline Mesh Communication (OMC) — Android Application

Decentralized, infrastructure-free peer-to-peer messaging for Android. Phones discover each other over BLE / Nearby Connections, connect via Wi-Fi Direct, relay messages multi-hop, survive disconnections with delay-tolerant store-and-forward, and confirm delivery with ACKs — **no internet, tower, or router required**.

## Quick Start (2 Phones, 5 Minutes)

1. Install the APK on both phones, grant Nearby / Bluetooth / Location / Notification permissions.
2. Turn on airplane mode, then Bluetooth ON.
3. Open OMC → set a display name → **Start Mesh**.
4. Each phone lists the other in **Peers** within ~10 s → open **Chat** → send.

Full steps: [`docs/06-Deployment-and-User-Manual.md`](docs/06-Deployment-and-User-Manual.md)

## Android App (Java Boilerplate)

Stock Android-Java skeleton, no features yet — ready to build on:

```
app/src/main/java/com/example/omc/MainActivity.java
app/src/main/AndroidManifest.xml
app/src/main/res/layout/activity_main.xml
app/src/main/res/values/{strings,colors,themes}.xml
app/src/test/.../ExampleUnitTest.java
app/src/androidTest/.../ExampleInstrumentedTest.java
```

- Open this folder in Android Studio (Hedgehog+, JDK 17) and let it sync; or run `./gradlew assembleDebug` once the Gradle wrapper jar is present (`gradle wrapper` generates it).
- `applicationId = com.example.omc`, `minSdk 26`, `targetSdk/compileSdk 34`.

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

## Submission Checklist

- [ ] Fill all `____________` blanks (names, dates, device matrix, measured results).
- [ ] Redraw RPD Gantt in GanttProject/Excel for the printed copy.
- [ ] Attach screenshots (Doc 07 App. D) and signed test cycles (Doc 05 §9).
- [ ] Render Doc 07 into the institutional template before printing.
