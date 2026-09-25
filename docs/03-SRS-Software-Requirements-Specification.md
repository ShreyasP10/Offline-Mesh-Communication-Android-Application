# Document 03 — Software Requirements Specification (SRS)

## OMC: Offline Mesh Communication Android Application

| Field | Detail |
|---|---|
| **Document ID** | OMC-SRS-1.0 |
| **Version** | 1.0 |
| **Date** | September 2026 |
| **Authors** | Shreyas Pawar, Veer Shah, Sayal Shah, Dhanashri Adawade |
| **Approver** | Internal Guide |
| **Standard** | IEEE 830-1998 (Recommended Practice for SRS) |
| **Status** | Approved |

---

## Table of Contents

1. Introduction
2. Overall Description
3. System Features (Functional Requirements)
4. External Interface Requirements
5. Non-Functional Requirements
6. User Stories and Use Cases
7. System Constraints, Assumptions, Dependencies
8. Acceptance Criteria
9. Traceability Matrix
10. Appendices

---

## 1. Introduction

### 1.1 Purpose

This SRS defines the complete functional and non-functional requirements for the **Offline Mesh Communication (OMC)** Android application — a zero-infrastructure, peer-to-peer messaging system using BLE discovery, Wi-Fi Direct transfer, multi-hop routing, and delay-tolerant store-and-forward.

Audience: developers, testers, guide, review committee. Every requirement has a unique ID (FR-x / NFR-x) used for traceability into the SDD (design), test plan (verification), and final report.

### 1.2 Scope

**Product:** A standalone Android app (no backend server) that lets nearby smartphones discover each other, connect peer-to-peer, exchange text messages directly or via multi-hop relays, survive disconnections via local persistence, and confirm delivery via ACKs — all with no internet, cell tower, or router.

**In scope:** discovery, connection, 1-hop and multi-hop messaging, broadcast, DTN store-and-forward, ACKs, heartbeat/self-heal, background foreground-service operation, local persistence, diagnostics, settings.

**Out of scope:** iOS, voice/video/images, E2EE (beyond optional signing), group rooms, LoRa hardware, cloud relay. See Proposal §6.2.

### 1.3 Definitions, Acronyms, Abbreviations

| Term | Meaning |
|---|---|
| Node | One device running OMC, identified by `node_id` (UUID persisted on first launch) |
| Neighbor | A node with an active direct P2P link to this node |
| Hop | One relay step (A→B is 1 hop) |
| TTL | Time-To-Live: max remaining hops a packet may travel; decremented per relay |
| DTN | Delay-Tolerant Networking: store-carry-forward when no contemporaneous path exists |
| ACK | Acknowledgement packet confirming delivery to destination |
| RREQ / RREP | Route Request / Reply (reserved for v2 AODV; v1 uses flooding) |
| HELLO / HEARTBEAT | Control packets for introduction and keepalive |
| Dedup cache | LRU set of recently seen `packet_id`s to suppress duplicate forwards |
| Foreground service | Android service with persistent notification, exempt from most background kills |

### 1.4 References

1. IEEE Std 830-1998, SRS Recommended Practice.
2. K. Fall, DTN Architecture, SIGCOMM 2003.
3. RFC 3561, AODV Routing.
4. Google Developers, Nearby Connections API.
5. Android Developers, Bluetooth LE + Wi-Fi Direct guides.
6. OMC Doc 01 (Proposal), Doc 02 (RPD), Doc 04 (SDD).

### 1.5 Overview

§2 describes the product context; §3 lists functional requirements by feature; §4 covers interfaces; §5 quantifies non-functional targets; §6 gives user stories; §7 states constraints; §8 defines acceptance; §9 traces requirements to tests.

---

## 2. Overall Description

### 2.1 Product Perspective

- **Self-contained system:** No server, no cloud, no admin. Each phone is simultaneously host, router, and client.
- **Context diagram:** User ↔ OMC App ↔ (BLE + Wi-Fi Direct radios) ↔ Peer Devices. No external system is required; airplane-mode operation is the norm.
- **Interfaces to platform:** Android BLE advertiser/scanner, Nearby Connections client, Room DB, Foreground Service, Notification Manager.

### 2.2 Product Functions (Summary)

1. Advertise presence and scan for peers.
2. Establish and maintain P2P links with HELLO handshake.
3. Compose, send, receive, persist, and display text messages.
4. Forward packets multi-hop with TTL and dedup.
5. Store undeliverable messages and flush on opportunity.
6. Generate/match ACKs and surface delivery status.
7. Detect dead neighbors via heartbeat and re-route.
8. Run persistently in background; resume after reboot (if enabled).

### 2.3 User Classes and Characteristics

| Class | Description | Technical Skill | Priority |
|---|---|---|---|
| General user | Sends/receives messages in emergency | Low — needs 3-tap flow | High |
| Power user | Tunes TTL, expiry, views diagnostics | Medium | Medium |
| Tester / Evaluator | Runs scripted multi-device scenarios | High | Medium (project phase) |

All classes use the same APK; tester features live behind the Diagnostics screen, not a separate build.

### 2.4 Operating Environment

| Item | Requirement |
|---|---|
| OS | Android 8.0 (API 26) through Android 14 (API 34) |
| Hardware | BLE 4.0+, Wi-Fi Direct capable radio |
| Storage | ≥ 50 MB free |
| RAM | ≥ 2 GB recommended |
| Network | None required; validated in airplane mode with BT/location on |
| Display | 4–8 inch, portrait; adaptive for larger |

### 2.5 Design and Implementation Constraints

- C-1: No root access may be required.
- C-2: No external hardware (LoRa, router) may be required.
- C-3: Must comply with Android background-execution limits (foreground-service types declared).
- C-4: Must comply with Play Store permission policies if published (declare `BLUETOOTH_SCAN/CONNECT/ADVERTISE`, `ACCESS_FINE_LOCATION` where needed, `NEARBY_WIFI_DEVICES`, `FOREGROUND_SERVICE_*`).
- C-5: Kotlin + Gradle build; minSdk 26, targetSdk 34.
- C-6: All mesh traffic stays on local radios; no internet transmission of message content.

### 2.6 Assumptions and Dependencies

- A-1: Users grant all runtime permissions (app degrades gracefully if denied — see FR-1.5).
- A-2: Devices are at least intermittently within radio range (~10–30 m BLE discovery; Wi-Fi Direct transfer range varies).
- A-3: At least 2 physical devices available for core claims; 3–5 for multi-hop claims.
- A-4: Google Play Services available for Nearby Connections API (standard on GMS devices; limitation noted for AOSP-only devices).
- A-5: Device clocks need not be synchronized (TTL is hop-based, not time-based; expiry uses local monotonic + wall-clock with skew tolerance).

---

## 3. System Features — Functional Requirements

> Convention: **shall** = mandatory for acceptance; **should** = desired. Each FR maps to test IDs in Doc 05.

### FR-1: Peer Discovery

| ID | Requirement | Priority |
|---|---|---|
| FR-1.1 | The system **shall** advertise a unique `node_id` plus display name via Nearby/BLE while mesh is started. | High |
| FR-1.2 | The system **shall** continuously scan for nearby OMC nodes while mesh is started. | High |
| FR-1.3 | The system **shall** display discovered nodes with name, proximity/connection status, and last-seen time on the Peers screen. | High |
| FR-1.4 | The system **shall** adapt scan/advertise intervals (aggressive when screen on / sparse when idle) to conserve battery. | Medium |
| FR-1.5 | If Bluetooth is off or permissions denied, the system **shall** show a non-crashing explanatory prompt with a one-tap path to settings. | High |
| FR-1.6 | The system **shall** stop advertising/scanning within 2 s of the user stopping the mesh. | Medium |

### FR-2: Connection Management

| ID | Requirement | Priority |
|---|---|---|
| FR-2.1 | The user **shall** be able to request a P2P connection to any discovered node. | High |
| FR-2.2 | The system **shall** auto-accept valid incoming OMC connection requests (no manual pairing code in v1). | High |
| FR-2.3 | On connection establishment, both endpoints **shall** exchange HELLO (node_id, display name, protocol version); version mismatch **shall** be rejected with a logged warning. | High |
| FR-2.4 | The system **shall** maintain a neighbor table (node_id ↔ endpoint_id, last_seen, RSSI if available, status). | High |
| FR-2.5 | The system **shall** remove a neighbor and fire a re-route event when the link drops or heartbeat times out (§FR-7). | High |
| FR-2.6 | The system **shall** support ≥ 10 concurrent neighbors per node without crash (degraded throughput acceptable). | Medium |

### FR-3: Messaging (1-Hop + UI)

| ID | Requirement | Priority |
|---|---|---|
| FR-3.1 | The user **shall** compose and send a text message (1–4000 chars) to a selected node or to all peers (broadcast). | High |
| FR-3.2 | The system **shall** serialize every message into an OMC/1.0 packet with unique `packet_id` (UUIDv4), header, and payload. | High |
| FR-3.3 | The system **shall** display per-message status: PENDING (queued) → SENT (handed to transport) → DELIVERED (ACK received) → FAILED (retries exhausted) / EXPIRED. | High |
| FR-3.4 | The system **shall** persist all sent/received/pending messages locally (Room) so killing the app loses no history. | High |
| FR-3.5 | The system **shall** reject empty messages client-side and > 4 KB messages with a clear error (no silent truncation). | Medium |
| FR-3.6 | Incoming messages **shall** appear in the Chat list with sender name, text, timestamp, and hop count (if relayed). | High |

### FR-4: Multi-Hop Routing

| ID | Requirement | Priority |
|---|---|---|
| FR-4.1 | The system **shall** forward packets toward non-neighbor destinations using flooding with TTL (default 10, user-configurable 1–15). | High |
| FR-4.2 | Each relay **shall** decrement TTL by 1 and increment `hop_count` by 1. | High |
| FR-4.3 | The system **shall** suppress duplicate forwards using a bounded LRU `seenCache` of `packet_id` (default capacity 1000). | High |
| FR-4.4 | Packets with TTL ≤ 0 after decrement **shall** be dropped (and counted in diagnostics). | High |
| FR-4.5 | Broadcast packets (destination = `BROADCAST`) **shall** be delivered locally AND re-flooded once (subject to TTL/dedup). | High |
| FR-4.6 | The system **shall not** forward a packet back out the endpoint it arrived on (split-horizon for 1-hop loops). | High |

### FR-5: DTN Store-and-Forward

| ID | Requirement | Priority |
|---|---|---|
| FR-5.1 | If no route/neighbor leads toward the destination, the system **shall** persist the packet in the pending queue instead of dropping it. | High |
| FR-5.2 | On neighbor-added or periodic sweep (default 30 s), the system **shall** re-attempt delivery of matching pending packets. | High |
| FR-5.3 | Each stored packet **shall** carry `expires_at` (default 24 h, configurable); expired packets transition to EXPIRED and are purged. | Medium |
| FR-5.4 | Pending-queue ordering **shall** be FIFO per destination (oldest first) to preserve causal order in the common case. | Medium |
| FR-5.5 | The Diagnostics screen **shall** show pending count and oldest pending age. | Medium |

### FR-6: Delivery Acknowledgement

| ID | Requirement | Priority |
|---|---|---|
| FR-6.1 | The destination node **shall** emit an ACK packet for every DATA packet flagged `ACK_REQUIRED` (default on for unicast, off for broadcast). | High |
| FR-6.2 | On receiving a matching ACK (`ack_for_packet_id`), the origin **shall** mark the message DELIVERED and remove it from the pending queue. | High |
| FR-6.3 | If no ACK arrives within timeout (default 10 s, exponential backoff), the origin **shall** retry up to N times (default 3) before marking FAILED. | High |
| FR-6.4 | Intermediate relays **shall not** consume ACKs addressed to another node; they forward ACKs like DATA (TTL applies). | High |

### FR-7: Self-Healing (Heartbeat + Failure Detection)

| ID | Requirement | Priority |
|---|---|---|
| FR-7.1 | Every node **shall** broadcast HEARTBEAT every T_hb seconds (default 4 s, configurable 2–10 s). | High |
| FR-7.2 | A neighbor silent for T_dead seconds (default 12 s = 3× T_hb) **shall** be marked DEAD and removed from the routable set. | High |
| FR-7.3 | On neighbor-dead, the system **shall** trigger re-route: pending packets via that neighbor return to the queue for alternate forwarding. | High |
| FR-7.4 | Heartbeat interval and dead timeout **shall** be user-configurable within safe bounds (enforced T_dead ≥ 2× T_hb). | Low |

### FR-8: Security (Baseline for v1)

| ID | Requirement | Priority |
|---|---|---|
| FR-8.1 | The system **shall** validate every inbound packet (schema, version, size bounds) and drop malformed packets with a logged counter (no crash). | High |
| FR-8.2 | The system **shall** support optional per-message signing (toggle in Settings; Ed25519 or HMAC-device-key in v1) and **shall** reject tampered signed packets. | Medium |
| FR-8.3 | The system **shall not** transmit message content over the internet; all payloads stay on local P2P links. | High |

### FR-9: Background Operation and Lifecycle

| ID | Requirement | Priority |
|---|---|---|
| FR-9.1 | The system **shall** run mesh networking in a foreground service with a persistent notification (showing peer/message counts) and an action to stop. | High |
| FR-9.2 | If enabled in Settings, the system **shall** auto-start the mesh after device reboot (BOOT_COMPLETED receiver). | Medium |
| FR-9.3 | The system **shall** preserve UI state (draft, scroll, selected peer) across rotation and process recreation. | Medium |
| FR-9.4 | The system **shall** release radios promptly on mesh stop (no lingering advertisers/scanners — verified in tests). | Medium |

### FR-10: Diagnostics and Settings

| ID | Requirement | Priority |
|---|---|---|
| FR-10.1 | The Diagnostics screen **shall** show: node_id, protocol version, connected peers, routing/neighbor table, pending queue, seen-cache size, TTL/dedup drop counters, heartbeat status. | Medium |
| FR-10.2 | Settings **shall** expose: display name, TTL, heartbeat interval, message expiry, auto-start on boot, signing toggle — each with safe defaults and bounds. | Medium |
| FR-10.3 | The app **shall** provide an in-app log share (export recent mesh log) to aid field debugging. | Low |

---

## 4. External Interface Requirements

### 4.1 User Interfaces

- **Screens:** Home (mesh on/off + status), Peers (discovered/connected list), Chat (per-peer + broadcast), Diagnostics, Settings, Onboarding (permissions + display name, ≤ 2 screens).
- **Navigation:** Bottom nav or drawer; Chat reachable in ≤ 2 taps from Home; send in ≤ 3 taps total.
- **Status language:** ⏳ Pending / ➤ Sent / ✓ Delivered / ✗ Failed — with text labels (not color-only) for accessibility.
- **No internet UI:** The app must never show a blocking "no internet" error; offline is the expected state.

### 4.2 Hardware Interfaces

- BLE advertiser/scanner via Android Bluetooth APIs (abstracted by Nearby Connections where possible).
- Wi-Fi Direct / Wi-Fi Aware transport via Nearby Connections `P2P_CLUSTER`.
- Local SQLite storage via Room (no external SD-card dependency).

### 4.3 Software Interfaces (External APIs Used)

| API | Use |
|---|---|
| `ConnectionsClient.startAdvertising / startDiscovery` | Peer discovery |
| `requestConnection / acceptConnection` | Link setup |
| `sendPayload (BYTES)` | Packet transfer (JSON ≤ 4 KB + overhead) |
| `PayloadCallback.onPayloadReceived` | Inbound path |
| `BluetoothAdapter / Advertiser / Scanner` (fallback diagnostics) | Permission/radio state checks |
| Room `RoomDatabase` | `Node`, `Neighbor`, `Message`, `Ack` entities |
| `ForegroundService + NotificationManager` | Background operation |

Wire format detail is normative in Doc 04 (SDD §7), not here.

### 4.4 Communication Interfaces

- All inter-node traffic uses binary BYTES payloads containing UTF-8 JSON (OMC/1.0). No HTTP, no sockets to a server, no cloud endpoint.
- Maximum application payload: 4,096 bytes message text; total packet bounded (header + payload) and validated on receipt.

---

## 5. Non-Functional Requirements

### 5.1 Performance

| ID | Metric | Target | Measurement |
|---|---|---|---|
| NFR-P1 | First-peer discovery time | ≤ 10 s (typical indoor, 2 devices) | Field log |
| NFR-P2 | Connection setup (tap → HELLO done) | ≤ 5 s | Field log |
| NFR-P3 | 1-hop message latency (send → display) | ≤ 500 ms | Instrumented log |
| NFR-P4 | 3-hop message latency | ≤ 3 s | Instrumented log |
| NFR-P5 | Sustained throughput (1-hop bulk) | ≥ 100 KB/s | Bulk-send harness |
| NFR-P6 | Max message size | 4 KB supported; larger rejected cleanly | TC-M-05/06 |

### 5.2 Reliability

| ID | Requirement |
|---|---|
| NFR-R1 | Delivery ratio ≥ 90% in static 3-node line topology (100 trial messages). |
| NFR-R2 | Zero infinite forwarding loops in any topology (TTL + dedup + split-horizon jointly guarantee termination). |
| NFR-R3 | No message loss on app kill for persisted states (kill → relaunch → history intact). |
| NFR-R4 | Graceful degradation: link loss never crashes the app; affected messages return to PENDING. |

### 5.3 Scalability

| ID | Requirement |
|---|---|
| NFR-S1 | ≥ 10 concurrent neighbors without crash. |
| NFR-S2 | Reliable delivery across ≥ 5 hops in line topology (latency target relaxed). |
| NFR-S3 | Seen-cache and pending queue bounded (no unbounded memory growth; LRU + expiry). |

### 5.4 Battery and Resource

| ID | Requirement |
|---|---|
| NFR-B1 | Active mesh ≤ 8%/hr drain (screen off, mesh on, nominal heartbeat). |
| NFR-B2 | Idle background ≤ 3%/hr. |
| NFR-B3 | APK size ≤ 50 MB (debug); release with R8 ≤ 25 MB target. |

### 5.5 Security and Privacy

| ID | Requirement |
|---|---|
| NFR-SEC1 | Malformed-packet survival: 10k fuzzed packets cause 0 crashes, 100% dropped+counted. |
| NFR-SEC2 | No PII beyond display name + node_id is broadcast; no location is embedded in packets. |
| NFR-SEC3 | Optional signing defaults off (usability) but available; when on, tampered packets rejected. |

### 5.6 Usability

| ID | Requirement |
|---|---|
| NFR-U1 | Onboarding ≤ 2 screens; send-a-message ≤ 3 taps post-onboarding. |
| NFR-U2 | All permission denials produce guidance, never a dead end or crash. |
| NFR-U3 | Accessibility: status not color-only; touch targets ≥ 48 dp; content descriptions on icon buttons. |

### 5.7 Maintainability and Portability

| ID | Requirement |
|---|---|
| NFR-M1 | Modular packages (`mesh/`, `discovery/`, `connection/`, `protocol/`, `storage/`, `service/`, `ui/`) with documented public APIs (Doc 04 §8). |
| NFR-M2 | Unit coverage ≥ 70% on `protocol/`, `mesh/`, `storage/`. |
| NFR-M3 | Portable across API 26–34 and 4–8" screens; OEM-specific workarounds isolated in one module with comments. |

---

## 6. User Stories and Use Cases

### 6.1 User Stories

| ID | As a… | I want… | So that… | Maps To |
|---|---|---|---|---|
| US-1 | Victim | Send text to a family member without internet | They know I'm safe | FR-3, FR-4 |
| US-2 | Responder | Broadcast an alert to all nearby devices | Everyone is warned at once | FR-3.1, FR-4.5 |
| US-3 | Trekker | See nearby OMC users | I can coordinate the group | FR-1.3 |
| US-4 | User | See ✓ Delivered on my message | I know it arrived | FR-6 |
| US-5 | User | Have messages delivered after a disconnect | Temporary gaps don't lose data | FR-5 |
| US-6 | User | Keep battery drain low | My phone lasts the emergency | NFR-B1/B2 |
| US-7 | Tester | View routing/pending/heartbeat state | I can verify multi-hop claims | FR-10.1 |

### 6.2 Use-Case Specifications (Abridged)

**UC-1: Send 1-to-1 message**
Actor: User. Pre: mesh started, ≥ 1 neighbor or route. Flow: select peer → type → Send → system builds packet → routes/stores → ACK → status ✓. Alt: no route → PENDING + stored (FR-5). Post: message persisted.

**UC-2: Broadcast alert**
Actor: Responder. Flow: open Broadcast → type → Send → floods once per node (dedup) → all in range display it. ACK not required by default.

**UC-3: Delayed delivery (DTN)**
Actor: System + User. Pre: destination offline. Flow: send → stored → destination appears → auto-flush → delivered. User sees PENDING throughout; no resend needed.

**UC-4: Node failure recovery**
Trigger: heartbeat timeout. Flow: mark DEAD → re-queue affected → alternate path or hold. User-visible effect limited to transient PENDING.

---

## 7. Constraints, Assumptions, Dependencies — Consolidated

See §2.5–2.6. Additional explicit limitations for evaluators:

1. Nearby Connections requires Google Play Services; pure-AOSP devices without GMS are not claimed as supported in v1.
2. Radio range and throughput vary by device, orientation, and RF environment; NFR targets are validated under stated field conditions (Doc 05 §4), not as universal guarantees.
3. This system is a **complement to**, not a replacement for, official emergency services (no automatic location dispatch in v1).

---

## 8. Acceptance Criteria (Gate for Project Sign-Off)

The project is **accepted** iff ALL of the following pass on physical devices in airplane mode:

1. **AC-1 (Discovery):** Two devices discover each other ≤ 10 s (TC-D-01).
2. **AC-2 (1-hop chat):** Text exchanged bidirectionally ≤ 500 ms typical (TC-M-01).
3. **AC-3 (Multi-hop):** Message traverses A→B→C (2 hops) and displays with correct hop count (TC-R-01).
4. **AC-4 (DTN):** Message sent while D offline is auto-delivered on D's return, no manual resend (TC-N-01/02).
5. **AC-5 (ACK):** Sender shows DELIVERED after destination ACK (TC-A-01).
6. **AC-6 (Self-heal):** Killing an intermediate peer marks it dead ≤ 12 s and re-routes or holds pending without crash (TC-H-02/03).
7. **AC-7 (Robustness):** Malformed-packet injection causes no crash (TC-S-01); app-kill loses no history (TC-M-03).
8. **AC-8 (Quality):** Critical + High test cases 100% pass; core coverage ≥ 70%.

---

## 9. Traceability Matrix (Requirement → Design → Test)

| Requirement | SDD Section | Test IDs |
|---|---|---|
| FR-1 Discovery | §2–3, §8 DiscoveryApi | TC-D-01–05 |
| FR-2 Connection | §3–4, §9.1 | TC-C-01–05 |
| FR-3 Messaging | §4–5, §7 packet | TC-M-01–07 |
| FR-4 Routing | §7.4 algorithm | TC-R-01–06 |
| FR-5 DTN | §7.5 algorithm | TC-N-01–04 |
| FR-6 ACK | §7 + AckManager | TC-A-01–03 |
| FR-7 Self-heal | §7.6 + HeartbeatMgr | TC-H-01–03 |
| FR-8 Security | Signer/Verifier | TC-S-01–03 |
| FR-9 Background | MeshForegroundService | TC-E-04 + field |
| FR-10 Diag/Settings | UI layer | Manual + TC-E-05 |
| NFR-P1–P6 | §2 arch + §7 | TC-P-01–05 |
| NFR-R/SEC/B/U/M | Respective SDD + tests | Doc 05 §5–6 |

---

## 10. Appendices

### A. Open Issues (Must Be Closed by Design Gate W4)

1. Finalize Nearby strategy constant (`P2P_CLUSTER`) and service-ID string.
2. Confirm default TTL (10) and heartbeat/dead (4 s / 12 s) after first RF measurements — values tunable without protocol change.
3. Decide signing algorithm for v1 (Ed25519 vs HMAC) — defaults off either way.

### B. Glossary — See §1.3.

### C. Wireframes (Placeholder — Attach Screenshots in Final Submission)

- Home, Peers, Chat, Diagnostics, Settings, Onboarding (2 screens).

*Next document: 04 — Software Design Document (SDD).*
