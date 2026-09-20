# Document 01 — Project Proposal / Synopsis

## OMC: Offline Mesh Communication Android Application
### A Decentralized, Infrastructure-Free Peer-to-Peer Messaging System

| Field | Detail |
|---|---|
| **Project Title** | Offline Mesh Communication Android Application (OMC) |
| **Domain** | Mobile Computing / Wireless Ad-hoc Networks / Delay-Tolerant Networking |
| **Project Type** | Final Year Major Project (B.E. / B.Tech) |
| **Duration** | 16 Weeks (One Semester) |
| **Team Size** | 4 Members |
| **Guide** | ____________ (Designation, Department) |
| **Institution** | ____________ |
| **Academic Year** | 2025–26 |
| **Version** | 1.0 — Submitted for Faculty / Committee Approval |
| **Date** | ____________ |

> **How to use this document:** Fill in the blanks (names, enrollment numbers, institution). Submit as-is for synopsis approval. Do not edit the technical scope without guide approval once signed.

---

## 1. Title and Keywords

**Title:** Design and Implementation of a Decentralized Offline Mesh Communication System for Android Devices Using Bluetooth Low Energy and Wi-Fi Direct with Delay-Tolerant Store-and-Forward Routing.

**Keywords:** Mesh Network, MANET, Delay-Tolerant Networking (DTN), Bluetooth Low Energy, Wi-Fi Direct, Nearby Connections API, Multi-hop Routing, Android, Disaster Communication.

---

## 2. Team Composition

| Role | Name | Enrollment No. | Responsibility (Proposed) |
|---|---|---|---|
| Team Lead | ____________ | ____________ | Architecture, protocol spec, integration |
| Member 2 | ____________ | ____________ | Discovery + connection module |
| Member 3 | ____________ | ____________ | Routing + DTN store-and-forward |
| Member 4 | ____________ | ____________ | UI + storage + testing |
| Internal Guide | ____________ | — | Weekly review, academic direction |
| External Mentor (if any) | ____________ | — | Industry feedback |

---

## 3. Abstract (250–300 words)

Modern mobile communication is entirely dependent on centralized infrastructure — cellular towers, fiber backhaul, and cloud servers. During natural disasters (floods, earthquakes), network congestion (large gatherings, festivals), remote operations (trekking, mining, defence), or deliberate blackouts, this infrastructure fails or becomes unavailable, leaving users with no fallback communication channel precisely when communication is most critical.

This project proposes **OMC (Offline Mesh Communication)**, an Android application that converts every smartphone into an autonomous network node. Using **Bluetooth Low Energy (BLE) for peer discovery** and **Wi-Fi Direct (via Google Nearby Connections API) for high-throughput data transfer**, devices self-organize into a **peer-to-peer mesh topology** without any internet, router, cell tower, or root access. Messages are relayed **hop-by-hop** across intermediate devices to reach destinations beyond direct radio range. When no end-to-end path exists, the system operates as a **Delay-Tolerant Network (DTN)** — messages are persistently stored locally and forwarded opportunistically when a path becomes available. Delivery acknowledgements (ACKs), Time-To-Live (TTL), duplicate suppression, heartbeat-based failure detection, and self-healing re-routing ensure reliability.

The expected outcome is a working Android application (API 26+), a documented application-layer protocol specification (OMC/1.0), and empirical evaluation of delivery ratio, latency, hop count, throughput, and battery consumption on 2–5 real devices. The system is software-only, zero-cost, and requires no additional hardware, making it deployable by any group of Android users in an emergency.

---

## 4. Problem Statement

> Centralized mobile communication has a single point of failure: the infrastructure itself. When towers are destroyed, congested, absent, or shut down, ordinary smartphone users have **no software-only, zero-cost fallback** for local communication. Dedicated hardware (walkie-talkies, satellite messengers, LoRa devices) is expensive, requires pre-planning, and is not available to the general public during an突发 emergency.

**Problem in one sentence:** How can unmodified Android smartphones communicate reliably with each other — including beyond direct radio range and across temporary disconnections — with zero infrastructure and zero additional hardware?

**Sub-problems:**

1. How to discover nearby devices without internet or a central directory?
2. How to establish peer-to-peer links using only built-in radios?
3. How to deliver messages beyond direct radio range (multi-hop)?
4. How to survive temporary disconnections (no contemporaneous end-to-end path)?
5. How to confirm delivery and recover from node mobility/failure?
6. How to do all of the above within Android background-execution and battery limits?

---

## 5. Objectives

### 5.1 Primary Objectives

| ID | Objective | Success Criterion |
|---|---|---|
| O-1 | Enable infrastructure-free peer discovery | Two devices discover each other within 10 s in airplane mode |
| O-2 | Establish P2P data connections | Connection setup ≤ 5 s, HELLO exchange populates neighbor table |
| O-3 | Implement multi-hop message forwarding | Message traverses ≥ 3 hops (A→B→C→D) correctly |
| O-4 | Implement DTN store-and-forward | Message stored while destination offline, delivered on reconnect |
| O-5 | Provide end-to-end delivery acknowledgement | Sender UI transitions PENDING → DELIVERED on ACK |
| O-6 | Achieve self-healing topology | Dead peer detected ≤ 12 s; pending messages re-routed |
| O-7 | Operate in background within battery budget | ≤ 8%/hr active, ≤ 3%/hr idle |
| O-8 | Provide emergency-usable UI | Any user sends a message in ≤ 3 taps after onboarding |

### 5.2 Secondary Objectives

- Document a reproducible application-layer protocol (OMC/1.0) with packet format, routing, and DTN algorithms.
- Produce empirical performance data (latency, throughput, delivery ratio, battery).
- Achieve ≥ 70% unit-test coverage on core modules.
- Publish code and docs as open source for future batches.

---

## 6. Scope

### 6.1 In Scope (Committed for This Project)

- Android 8.0 (API 26) through Android 14 (API 34).
- Text messaging up to 4 KB per message.
- One-to-one (unicast) and all-peers (broadcast) messaging.
- Flooding-based multi-hop routing with TTL and duplicate suppression (v1).
- Persistent local storage (Room/SQLite) with message expiry.
- Delivery ACKs with timeout and bounded retries.
- Heartbeat keepalive and dead-node detection.
- Foreground-service background operation with persistent notification.
- Diagnostics screen (node ID, peers, routing table, pending queue).
- Optional lightweight packet signing (toggleable).

### 6.2 Out of Scope (Explicitly Excluded — Future Work)

| Excluded Item | Reason | Future Path |
|---|---|---|
| iOS support | No Wi-Fi Direct equivalent; Multipeer Connectivity is a separate stack | Native iOS app + BLE bridge |
| Voice / video / images | Bandwidth and DTN fragmentation complexity | Chunked transfer in v2 |
| Full end-to-end encryption | Key exchange and review overhead | X25519 + AES-GCM in v2 |
| Group chat rooms | Membership, admin, history sync complexity | v2 feature |
| LoRa / long-range radio | Requires external hardware | Hardware bridge module |
| Internet gateway / cloud relay | Contradicts offline-first goal | Optional gateway in v2 |

> Defining out-of-scope items explicitly prevents scope creep during evaluation.

---

## 7. Target Audience and Use Cases

| Audience | Scenario | Value Proposition |
|---|---|---|
| Disaster victims | Flood/earthquake, towers down | Contact family, request help |
| Emergency responders | NDRF, fire, medical teams | Coordinate without infrastructure |
| Trekkers / remote workers | Mountains, forests, mines | Off-grid group coordination |
| Event attendees | Festivals, stadiums, protests | Chat under congestion/blackout |
| Defence / field units | Tactical edge, no network | Local resilient messaging |
| Rural communities | No coverage villages | Free local communication |
| Researchers / students | MANET/DTN testbed | Open protocol + reproducible results |

**Representative Use Cases:**

- **UC-1:** A stranded victim sends "I am safe at location X" to a family member 2 hops away via strangers' phones relaying automatically.
- **UC-2:** A rescue coordinator broadcasts "Evacuation at Gate 3 in 10 min" to all nearby devices.
- **UC-3:** A trekker's message is stored on a companion's phone and delivered when the destination re-enters range.

---

## 8. Literature Background (Brief — Full Survey in Final Report)

1. **Fall (2003) — DTN Architecture:** Introduced store-and-forward bundle protocol for challenged networks. OMC adopts the DTN principle at the application layer.
2. **Perkins et al. (RFC 3561) — AODV:** On-demand distance-vector routing for ad-hoc networks. OMC v1 uses simpler flooding; AODV is the planned v2 upgrade.
3. **Bluetooth SIG Mesh:** Designed for provisioned IoT, not ad-hoc phone messaging. Unsuitable for zero-setup emergency use.
4. **Existing apps (Bridgefy, Briar, FireChat, Meshtastic, Serval):** Either proprietary, discontinued, hardware-dependent, or internet-dependent for some modes. None provides an open, documented, student-reproducible Android-only DTN mesh stack — the gap OMC fills.

---

## 9. Proposed Solution Overview

```
 ┌──────────┐    BLE adv/scan    ┌──────────┐    Wi-Fi Direct    ┌──────────┐
 │  Node A  │◄──────────────────►│  Node B  │◄──────────────────►│  Node C  │
 │ (sender) │     discovery      │ (relay)  │      transfer      │  (dest)  │
 └──────────┘                    └──────────┘                    └──────────┘
       │                               │                               │
       │  DATA (ttl=10) ──► forward ──►│──► forward (ttl=9) ──► deliver │
       │◄────────────── ACK ───────────┴───────────────────────────────┘
       │  (if no path: STORE → forward later when neighbor appears)    │
```

**Pipeline:** Discover (BLE/Nearby) → Connect (P2P + HELLO) → Route (flooding + TTL + dedup) → Store if unreachable (DTN) → ACK on delivery → Heartbeat/self-heal continuously → Foreground service keeps it alive.

---

## 10. Technical Feasibility

| Aspect | Feasibility | Evidence / Notes |
|---|---|---|
| BLE discovery | **High** | Standard Android APIs; Nearby Connections abstracts advertising + scanning |
| P2P transfer | **High** | Nearby Connections `P2P_CLUSTER` strategy is mature and documented |
| Multi-hop flooding | **High** | Simple, well-understood; no complex route computation |
| Store-and-forward | **High** | Room (SQLite) is mature; coroutines handle async |
| ACK subsystem | **High** | Request/response matching on `packet_id` |
| Background operation | **Medium** | Requires foreground service + battery-optimization exemption; OEM skins vary — mitigated by user-manual guidance |
| Battery budget | **Medium** | Adaptive scan intervals + heartbeat tuning; measurable via Battery Historian |
| Cross-platform (iOS) | **Low (v1)** | Acknowledged limitation; scoped to future work |

**Feasibility verdict:** All core risks are software-engineering risks (solvable within a semester), not research risks. No unsolved problem blocks completion.

---

## 11. Technology Stack (Proposed)

| Layer | Choice | Justification |
|---|---|---|
| Language | Kotlin | Official Android language; coroutines + null safety |
| UI | Jetpack Compose (fallback: XML) | Modern, less boilerplate |
| Discovery + Transfer | Google Nearby Connections API | Single API for BLE + Wi-Fi Direct; handles permissions/encryption of link |
| Serialization | JSON via Gson (v1) | Human-readable, easy to debug; Protobuf in v2 |
| Storage | Room (SQLite) | Type-safe, migration support, Flow integration |
| Concurrency | Kotlin Coroutines + Flow | Structured concurrency for mesh loops |
| Background | Foreground Service | Required for Android 8+ background BLE |
| Build | Gradle (Kotlin DSL) | Standard |
| VCS | Git + GitHub | Collaboration + review |
| Testing | JUnit4/5, MockK, Espresso, Robolectric | Unit + integration + UI |
| Docs/Diagrams | Draw.io, Markdown, LaTeX/Word | Academic deliverables |

**Minimum hardware:** 5 Android phones (API 26–34) for multi-node tests; laptops for development; power banks for field tests.

---

## 12. Expected Outcomes and Deliverables

| # | Deliverable | Format |
|---|---|---|
| D-1 | Working Android application | Signed APK + full source on GitHub |
| D-2 | Protocol specification OMC/1.0 | Markdown/PDF in `docs/` |
| D-3 | Test plan + test report |pass/fail matrix + metrics |
| D-4 | User manual + deployment guide | Step-by-step with screenshots |
| D-5 | Final dissertation | 60–100 pages per institutional format |
| D-6 | Demonstration | Live 3–5 node demo + recorded video |
| D-7 | Presentation deck | 15–20 slides + speaker notes (for viva) |

**Measurable claims for the final demo:** 2-node chat without internet; 3–4 node multi-hop relay; store-and-forward across disconnect; ACK display; dead-node self-heal.

---

## 13. Timeline Summary (16 Weeks)

| Phase | Weeks | Key Milestone |
|---|---|---|
| Literature + Requirements | 1–2 | Approved SRS |
| Protocol + Design | 3–4 | Frozen OMC/1.0 + SDD |
| Implementation (sprints) | 5–12 | 2-node chat → multi-hop → DTN → ACK → self-heal |
| Testing (integration + field) | 13–14 | Test report with metrics |
| Documentation + Demo | 15–16 | Dissertation, APK, viva demo |

(Detailed schedule with Gantt chart is in Document 02 — RPD.)

---

## 14. Risks (Summary — Full Register in RPD)

- Android background restrictions → foreground service + manual guidance.
- OEM battery killers (Xiaomi, Oppo, Vivo) → whitelisting instructions.
- Nearby Connections payload/connection limits → bounded neighbor count + fallback design.
- Scope creep → protocol freeze after Week 4 with formal change control.

---

## 15. References (Initial)

1. K. Fall, "A Delay-Tolerant Network Architecture for Challenged Internets," *Proc. SIGCOMM*, 2003.
2. C. Perkins, E. Belding-Royer, S. Das, "Ad hoc On-Demand Distance Vector (AODV) Routing," RFC 3561, 2003.
3. Google Developers, "Nearby Connections API Overview," 2024.
4. Bluetooth SIG, "Bluetooth Core Specification v5.3," 2021.
5. Android Developers, "Wi-Fi Direct (Wi-Fi P2P)," 2024.
6. IEEE Std 830-1998, "Recommended Practice for Software Requirements Specifications."

---

## 16. Approval

| Role | Name | Signature | Date |
|---|---|---|---|
| Team Lead | | | |
| Member 2 | | | |
| Member 3 | | | |
| Member 4 | | | |
| Internal Guide | | | |
| Project Coordinator / HOD | | | |

*Next document: 02 — Requirements Planning Document (RPD) with full schedule, Gantt, resources, and risk plan.*
