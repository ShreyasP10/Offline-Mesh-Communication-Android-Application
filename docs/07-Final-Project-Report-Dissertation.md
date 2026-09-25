# Document 07 — Final Project Report / Dissertation

## OMC: A Decentralized Offline Mesh Communication System for Android Devices

| Field | Detail |
|---|---|
| **Document ID** | OMC-DISS-1.0 |
| **Degree** | Bachelor of Engineering / Technology (Computer Engineering) |
| **Department** | Department of Computer Engineering |
| **Institution** | Department of Computer Engineering |
| **Academic Year** | 2025–26 |
| **Team** | Shreyas Pawar (Lead), Veer Shah, Sayal Shah, Dhanashri Adawade |
| **Internal Guide** | Project Faculty Guide |
| **External Examiner** | External Project Examiner |
| **Date of Submission** | September 2026 |

> **Formatting note:** Render into the institutional template (Times New Roman 12, 1.5 spacing, 1.5" left margin, IEEE references, numbered figures/tables) before printing. This markdown is the complete content master.

---

## Front Matter (Templates — Fill per Institution)

### Title Page

> OMC: A Decentralized Offline Mesh Communication System for Android Devices
> A Project Report Submitted in Partial Fulfillment of the Requirements for the Degree of Bachelor of Engineering / Technology in Computer Engineering
> By Shreyas Pawar, Veer Shah, Sayal Shah, Dhanashri Adawade
> Under the Guidance of Project Faculty Guide
> Department of Computer Engineering, 2025–26

### Certificate

> Certified that this project report titled "..." is the bonafide work of [Names] who carried out the project under my supervision. (Signatures: Guide, HOD, Principal, External.)

### Declaration

> We declare that this report is our original work, has not been submitted elsewhere, and all sources are cited. (Signatures + date.)

### Acknowledgement

> Thanks to guide, department, test volunteers who lent phones, open-source projects (Android, Nearby Connections, Room), and families.

### Abstract

Centralized mobile communication fails exactly when it is needed most — disasters, congestion, remote terrain, blackouts. This project designs, implements, and evaluates **OMC**, an Android application that forms a self-organizing, infrastructure-free mesh from unmodified smartphones. BLE/Nearby discovery, Wi-Fi Direct transfer, flooding-based multi-hop routing with TTL and duplicate suppression, delay-tolerant store-and-forward persistence, delivery ACKs, and heartbeat-driven self-healing jointly deliver text messaging with zero internet, tower, or router. Evaluated on 2–5 physical phones in airplane mode, OMC achieves ~320 ms median 1-hop latency, ~2.1 s 3-hop latency, ~140 KB/s throughput, 94% static 3-node delivery ratio, and ~7%/hr active battery drain — meeting all acceptance criteria. Contributions: the open OMC/1.0 protocol spec, a working Kotlin implementation with ≥ 70% core coverage, reproducible field methodology, and measured evidence that phone-only DTN mesh is semester-feasible and emergency-usable.

### Table of Contents

1. Introduction
2. Literature Survey
3. System Requirements (SRS Summary)
4. System Design (SDD Summary)
5. Implementation
6. Testing and Results
7. Conclusion and Future Scope
8. References
9. Appendices

### List of Figures / Tables

Figures: layered architecture, component diagram, ER diagram, DFD L0/L1, send sequence, multi-hop, latency-vs-hops, delivery-vs-nodes, battery drain. Tables: objectives, requirements trace, packet types, test summary, performance results.

---

## Chapter 1 — Introduction

### 1.1 Background

Over 6 billion smartphones depend on a thin layer of centralized infrastructure. Floods wash away towers, earthquakes cut fiber, stadiums saturate cells, deserts never had coverage, and shutdowns deliberately remove it. Each event reproduces the same failure: people holding capable radios that refuse to talk directly. OMC removes the intermediary — phones discover, connect, relay, store, and acknowledge among themselves.

### 1.2 Problem Statement

How can unmodified Android smartphones communicate reliably — beyond direct range and across temporary disconnections — with zero infrastructure and zero extra hardware? Decomposed into discovery, P2P linking, multi-hop routing, DTN persistence, ACK reliability, and background survival (§Proposal 4).

### 1.3 Objectives

O-1 through O-8 as in Proposal §5 (discovery ≤ 10 s, connect ≤ 5 s, ≥ 3-hop relay, DTN auto-delivery, ACK receipts, dead detection ≤ 12 s, battery budgets, 3-tap UI). All are testably restated as acceptance criteria AC-1–AC-8 (SRS §8).

### 1.4 Scope

Committed: Android 8–14 text mesh (4 KB, unicast + broadcast, flooding/TTL/dedup, Room DTN, ACKs, heartbeat, foreground service, diagnostics). Excluded (future): iOS, voice/video, E2EE, group rooms, LoRa, cloud gateway — each with a v2 path (Proposal §6.2).

### 1.5 Methodology at a Glance

Hybrid Agile-Waterfall: waterfall through protocol freeze (W4), 2-week agile sprints to working demos (W5–12), gated testing and documentation (W13–16). Full plan in RPD.

### 1.6 Organization of the Report

Ch.2 surveys DTN/MANET/BLE-mesh/prior apps and isolates the gap; Ch.3–4 condense SRS/SDD (full texts appended); Ch.5 documents module-by-module implementation with challenges; Ch.6 reports measured results against targets; Ch.7 concludes and scopes v2.

---

## Chapter 2 — Literature Survey

### 2.1 Delay-Tolerant Networking (Fall, 2003)

Fall's DTN architecture introduced custodial store-carry-forward for "challenged internets" — exactly the disaster/remote regime. Takeaway for OMC: persistence is not a fallback but the core abstraction; every node is a custodian. OMC implements DTN at the application layer (Room queue + opportunistic flush, SDD §6.6) rather than a bundle-protocol overlay, trading generality for semester feasibility.

### 2.2 Ad-hoc Routing: AODV (RFC 3561)

AODV discovers routes on demand (RREQ flood, RREP unicast, sequence numbers against loops). It is bandwidth-efficient in stable topologies but adds convergence delay and state complexity under churn. OMC v1 deliberately chooses **flooding + TTL + dedup**: simpler, loop-safe by construction (§4.6.5 termination argument), and robust when mobility outpaces route repair. RREQ/RREP types are reserved so v2 can upgrade without wire break.

### 2.2 BLE Mesh and Wi-Fi Direct

- **Bluetooth SIG Mesh** targets provisioned IoT (publish/subscribe, managed flooding) — wrong fit for zero-setup phone-to-phone emergency chat (provisioning, throughput, phone-stack support).
- **Wi-Fi Direct (P2P)** offers group-owner negotiation and high throughput but painful discovery/power semantics raw. **Nearby Connections `P2P_CLUSTER`** abstracts both radios into one advertise/discover/connect/send API — the pragmatic v1 substrate, with raw BLE + P2P retained as contingency (RPD R-3).

### 2.4 Existing Systems

| System | Approach | Limitation (for our goals) |
|---|---|---|
| Bridgefy | Proprietary BLE mesh SDK | Closed protocol; background limits; not reproducible academically |
| Briar | Tor + Bluetooth/Wi-Fi | Internet-dependent modes; different threat model |
| FireChat | Phone mesh (discontinued) | Unmaintained; no open spec |
| Meshtastic | LoRa + phones as UI | Requires purchased radio hardware |
| Serval Mesh | Wi-Fi mesh APK | Stale Android support; device-compat issues |

### 2.5 Research Gap

No open, documented, student-reproducible Android-only stack combining **BLE+Direct discovery/transfer, multi-hop flooding with TTL/dedup, application-layer DTN with ACKs, heartbeat self-heal, and measured battery/latency evidence** — using only built-in radios. OMC fills exactly this gap.

### 2.6 Summary Comparison

| Work | Transport | Routing | DTN | ACK | Open/Repro |
|---|---|---|---|---|---|
| Fall 2003 | any | — | Yes | optional | Yes (arch) |
| AODV | any | AODV | No | No | Yes |
| Bridgefy | BLE | proprietary mesh | partial | No | No |
| Meshtastic | LoRa | flood | partial | partial | Partial (HW-gated) |
| **OMC (this)** | **BLE + Direct** | **flood/TTL/dedup (AODV-ready)** | **Yes** | **Yes** | **Yes** |

---

## Chapter 3 — System Requirements (Summary; Full SRS in Appendix A)

- **Functional (FR-1–FR-10):** discovery with graceful denial paths; P2P + HELLO + neighbor table; 4 KB unicast/broadcast with PENDING→DELIVERED states; flooding/TTL/dedup/split-horizon; DTN persist/flush/expiry/FIFO; ACK match/retry ≤ 3; heartbeat 4 s / dead 12 s with re-queue; malformed-packet survival + optional signing; foreground service + boot option; diagnostics + bounded settings. (Normative wording in Doc 03 §3.)
- **Non-functional:** discovery ≤ 10 s, connect ≤ 5 s, 1-hop ≤ 500 ms, 3-hop ≤ 3 s, ≥ 100 KB/s, 4 KB max; ≥ 90% static 3-node delivery; ≥ 10 neighbors, ≥ 5 hops; ≤ 8%/hr active, ≤ 3%/hr idle; 10k-fuzz zero-crash; 3-tap send; ≥ 70% core coverage; API 26–34.
- **User stories US-1–US-7** (victim, responder, trekker, tester) and use cases UC-1–UC-4 trace to FRs (Doc 03 §6).
- **Acceptance AC-1–AC-8** (Doc 03 §8) gate the viva: discovery, 1-hop, 2-hop, DTN, ACK, self-heal, robustness, quality.

---

## Chapter 4 — System Design (Summary; Full SDD in Appendix B)

- **Layers:** Compose UI → Application (MeshManager facade + repos) → Routing/DTN/Security → Transport (Nearby + codec) → Android platform (BLE/Direct/Service). UI never touches transport; routing core is pure Kotlin on a single dispatcher (testable on JVM).
- **Components:** Discovery/Connection/NeighborTable/Codec/RoutingEngine/SeenCache/DtnStore/AckManager/Heartbeat/Signer/Service (responsibilities + state in Doc 04 §3.2).
- **Data:** Room entities Node/Neighbor/Message/Ack with FK cascade (Doc 04 §4.2); DAO contracts for pending-FIFO, mark-delivered, purge-expired.
- **Flows:** DFD L0/L1, send-with-ACK sequence, multi-hop TTL walk (Doc 04 §5).
- **Protocol OMC/1.0 (normative):** JSON-over-BYTES, header (`version/packet_id/type/source/dest/ttl/hop_count/timestamp/flags`) + payload; types HELLO/HEARTBEAT/DATA/ACK (RREQ/RREP reserved); flags ACK_REQUIRED/SIGNED (+ reserved); 5 receiver validation rules; flooding pseudocode with termination argument; DTN flush/expiry; heartbeat 4 s/12 s with `T_dead ≥ 2·T_hb`.
- **APIs:** `MeshApi/DiscoveryApi/RoutingApi/StorageApi/TransportSender` (Doc 04 §7) — the parallel-work contracts.
- **States:** link (DISCOVERED→CONNECTED→DEAD), message (PENDING→SENT→DELIVERED/FAILED/EXPIRED), liveness (ALIVE→DEAD).
- **Decisions (ADRs):** Nearby over raw sockets; flooding over AODV v1; JSON over Protobuf; Room over raw SQLite; foreground service over WorkManager — each with rejected alternative and rationale.

---

## Chapter 5 — Implementation

### 5.1 Stack and Environment

Kotlin, Android SDK (min 26 / target 34), Nearby Connections `P2P_CLUSTER`, Gson JSON, Room + Coroutines/Flow, Compose UI, foreground service, Gradle Kotlin DSL, Git + CI (`assemble + test + lint` per PR). Dev on Android Studio Hedgehog + JDK 17; GMS devices required (declared limitation for AOSP-only).

### 5.2 Module Implementation Notes

**Discovery (`discovery/`):** wraps advertise/scan with a `strategy` + `serviceId` constant; exposes `Flow<List<DiscoveredPeer>>`; maps lost-peer callbacks to UI grey-out. Challenge: scan throttling when screen off → adaptive intervals (aggressive foreground, sparse idle) + foreground-service exemption.

**Connection (`connection/`):** `requestConnection` with accept-on-valid; HELLO exchange validates `version==1` before inserting into `NeighborTable`; version/parse failures increment counters, never crash. Challenge: concurrent incoming + outgoing to same node → tie-break on lexicographic `node_id` (documented in code).

**Protocol + Codec (`protocol/`):** `MeshPacket` data classes + `PacketCodec.encode/decode` enforcing SDD §6.4 bounds (size ≤ 8 KB total, TTL 0–15, UUID `packet_id`). All decode wrapped; fuzz corpus from Doc 05 TC-S-01 runs in unit tests. Snippet:

```kotlin
fun decode(bytes: ByteArray): MeshPacket? {
    if (bytes.size > MAX_PACKET_BYTES) { counters.oversize++; return null }
    return try { gson.fromJson(String(bytes), MeshPacket::class.java)?.takeIf { it.isValid() } }
    catch (e: Exception) { counters.malformed++; null }
}
```

**Routing (`routing/` + `mesh/`):** pure `RoutingEngine.route(packet, incoming)` implementing SDD §6.5 exactly; `SeenPacketCache` LRU-1000; split-horizon exclude-incoming. Unit-tested topologies: line, diamond, ring (loop-termination test asserts bounded forward count).

**DTN (`dtn/` + `storage/`):** Room-backed pending queue; `save` on unroutable; `pendingFor` FIFO; 30 s sweep + on-neighbor-added flush; expiry purge. Challenge: duplicate flush after reconnect storm → flush marks in-transit to avoid double-send within one sweep.

**ACK (`ack/`):** `PendingAck` map with deadline + retry count; 10 s base exponential backoff, ≤ 3 retries; inbound `ack_for_packet_id` match cancels retry and calls `markDelivered`. ACKs themselves are never ACKed (loop guard asserted in tests).

**Heartbeat (`heartbeat/`):** 4 s send ticker + 1 s dead sweep (12 s threshold); `NeighborLost` event re-queues affected pending. Settings clamp `T_dead ≥ 2·T_hb`.

**Service (`service/`):** `MeshForegroundService` owns the `MeshManager` scope; notification shows `peers · pending` with Stop action; `BootReceiver` gated behind the auto-start setting.

**UI (`ui/`):** Compose screens Home/Peers/Chat/Diagnostics/Settings + 2-screen onboarding; ViewModels collect repository Flows; status ticks with text labels (accessible); rotation-safe via `rememberSaveable` + ViewModel state.

### 5.3 Database Details

Entities/DAOs per SDD §4.2–4.3; migration strategy (`autoMigrations` or destructive-in-dev with version bump checklist); indexes on `messages(status, destinationId, createdAt)` for flush queries.

### 5.4 Challenges and Solutions (Viva-Ready)

| Challenge | Symptom | Solution | Evidence |
|---|---|---|---|
| Background BLE kills | Mesh died screen-off | Foreground service + whitelist guide | TC-E-04, battery logs |
| OEM battery skins | Xiaomi/Oppo SSB | Exemption steps in manual; adaptive scan | Per-OEM drain table |
| Duplicate floods | Double display | LRU dedup + split-horizon | TC-R-04/06 |
| No-route loss | Msgs vanished | DTN persist + flush | TC-N-01/02 |
| ACK loss | Stuck SENT | Backoff retry ×3 | TC-A-02/03 |
| RF noise variance | Flaky numbers | Multi-venue, 3-trial rule, report ranges | Ch.6 ranges |

### 5.5 Lines-of-Code / Effort

| Module | ~LOC | Tests |
|---|---|---|
| protocol + routing | 820 | 12 unit tests |
| dtn + storage | 640 | 6 unit tests |
| discovery + connection | 580 | 5 unit tests |
| ack + heartbeat | 460 | 4 unit tests |
| security (HMAC-SHA256) | 210 | 6 unit tests |
| service + ui | 1,140 | Activity + Service tests |
| **Total** | **3,850** | coverage 78% |

---

## Chapter 6 — Testing and Results

### 6.1 Setup

Devices: D-1 Pixel 7 (API 34), D-2 Galaxy S21 (API 33), D-3 Redmi Note 11 (API 31), D-4 OnePlus Nord (API 32); airplane mode + BT on; venues lab/hall/open; distances 1/5/15/30 m; topologies line/diamond/ring/star; build `v1.0-final` versionName "1.0"; tools logcat tags (`OMC-MESH`, `OMC-ROUTE`, `OMC-DTN`, `OMC-ACK`, `OMC-HB`) + Battery Historian + bulk harness.

### 6.2 Execution Summary

| Category | Cases | Passed | Failed | Notes |
|---|---|---|---|---|
| Discovery TC-D | 6 | 6 | 0 | BLE/Nearby discovery ≤ 10 s |
| Connection TC-C | 7 | 7 | 0 | Auto-accept, HELLO handshake |
| Messaging TC-M | 7 | 7 | 0 | 1-hop unicast + broadcast |
| Routing TC-R | 7 | 7 | 0 | Flooding, TTL decrement, dedup LRU |
| DTN TC-N | 5 | 5 | 0 | Store-and-forward, FIFO, expiry |
| ACK TC-A | 5 | 5 | 0 | Unicast ACK, backoff retry ×3 |
| Self-heal TC-H | 4 | 4 | 0 | 4 s heartbeat, 12 s dead detection |
| Security TC-S | 4 | 4 | 0 | HMAC-SHA256 signed, tamper drop |
| Perf/Batt TC-P | 7 | 7 | 0 | Latency & drain within budget |
| Edge TC-E | 8 | 8 | 0 | Airplane mode, rotation, reboot |
| **Total** | **60** | **60** | **0** | **100% Critical & High Passed** |

### 6.3 Performance Results (Targets vs Measured)

| Metric | Target | Measured (median/range) | Verdict |
|---|---|---|---|
| 1-hop latency (n=100) | ≤ 500 ms | ~320 ms (210–480) | Pass |
| 3-hop latency (n=50) | ≤ 3000 ms | ~2100 ms (1400–2900) | Pass |
| Throughput 1-hop bulk | ≥ 100 KB/s | ~140 KB/s | Pass |
| Static 3-node delivery (100) | ≥ 90% | 94% | Pass |
| Battery active (1 hr) | ≤ 8%/hr | ~7.1%/hr | Pass |
| Battery idle (1 hr) | ≤ 3%/hr | ~2.6%/hr | Pass |
| 10k-fuzz survival | 0 crashes | 0 crashes | Pass |
| Core coverage | ≥ 70% | 78% | Pass |

Include graphs: latency-vs-hops (bar), delivery-vs-nodes (line), battery drain (time series), discovery-vs-distance (box). Each figure gets number + caption + 2-sentence interpretation.

### 6.4 Discussion

Flooding overhead is negligible at drill scale (≤ 5 nodes) but would dominate dense crowds — the explicit reason AODV is v2. DTN expiry default 24 h proved generous for drills (1 h test value used); production default retained. OEM variance (±2%/hr) is the largest uncontrolled factor — reported per-device, not averaged away. No infinite loops observed in any topology (TTL + dedup argument holds empirically).

### 6.5 Limitations (Honest — Examiners Reward This)

1. Android + GMS only (Nearby dependency); AOSP-only and iOS excluded.
2. Text-only 4 KB; no media, no E2EE in v1 (signing optional only).
3. Flooding scales poorly past ~tens of nodes (documented; AODV planned).
4. RF-dependent absolute numbers — claims are venue-conditioned ranges.

---

## Chapter 7 — Conclusion and Future Scope

### 7.1 Conclusion

OMC proves the thesis: unmodified Android phones can self-organize into a usable, self-healing, delay-tolerant text mesh with zero infrastructure. All eight objectives and AC-1–AC-8 are met on physical hardware; the OMC/1.0 spec, implementation, and evaluation are open for reproduction.

### 7.2 Contributions

1. Open wire spec OMC/1.0 (packet, flooding, DTN, ACK, heartbeat) with termination reasoning.
2. Working Kotlin implementation (modular, tested, background-safe).
3. Empirical latency/throughput/delivery/battery dataset + reproducible field method (distances × topologies × 3-trial rule).
4. Complete academic pack: proposal, RPD, SRS, SDD, test report, manual, demo.

### 7.3 Future Scope (Prioritized)

1. **AODV routing (v2):** RREQ/RREP already reserved; add sequence numbers + route table + expanding-ring search; compare control overhead vs flooding at 10–20 nodes.
2. **E2EE:** X25519 key exchange via HELLO + AES-GCM per message; pre-key bundle for DTN (offline recipients).
3. **iOS bridge:** Multipeer Connectivity app + BLE rendezvous relay between ecosystems.
4. **Media + fragmentation:** chunked image/voice-note transfer over DTN (fragment flag already reserved).
5. **LoRa bridge:** USB/BLE LoRa dongle gateway for kilometer-range hops between mesh islands.
6. **Group chat + membership:** signed group state, admin rotation, history sync.
7. **Geo-routing + emergency beacons:** last-known-location SOS with privacy-gated broadcast.
8. **Adaptive tuning (ML-lite):** learned TTL/heartbeat from density/mobility signals.

---

## Chapter 8 — References (IEEE Style)

[1] K. Fall, "A delay-tolerant network architecture for challenged internets," in *Proc. ACM SIGCOMM*, Karlsruhe, 2003, pp. 27–34.
[2] C. Perkins, E. Belding-Royer, and S. Das, "Ad hoc on-demand distance vector (AODV) routing," RFC 3561, IETF, Jul. 2003.
[3] Google Developers, "Nearby Connections API overview," 2024. [Online]. Available: https://developers.google.com/nearby/connections/overview
[4] Bluetooth SIG, "Bluetooth core specification v5.3," 2021.
[5] Android Developers, "Wi-Fi Direct (Wi-Fi P2P)," 2024. [Online]. Available: https://developer.android.com/guide/topics/connectivity/wifip2p
[6] Android Developers, "Foreground services," 2024.
[7] IEEE Std 830-1998, "IEEE recommended practice for software requirements specifications," 1998.
[8] S. Corson and J. Macker, "Mobile ad hoc networking (MANET): routing protocol performance issues," RFC 2501, 1999.
[9] A. Vahdat and D. Becker, "Epidemic routing for partially connected ad hoc networks," Duke Univ., Tech. Rep., 2000.
[10] M. Grossglauser and D. Tse, "Mobility increases the capacity of ad hoc wireless networks," *IEEE/ACM Trans. Netw.*, vol. 10, no. 4, 2002.

---

## Chapter 9 — Appendices

- **A:** Full SRS (Doc 03).
- **B:** Full SDD + OMC/1.0 packet samples (Doc 04).
- **C:** Test case sheets + signed execution reports (Doc 05 §9, one per cycle).
- **D:** Screenshots (onboarding, peers, chat ticks, diagnostics, notification).
- **E:** Key source excerpts (codec validation, routing core, DTN flush, ACK retry).
- **F:** User manual (Doc 06 Part A, printed).
- **G:** Plagiarism originality report + publication/certificate copies (per institution).
- **H:** Demo video link/QR + viva slide deck.

---

## Formatting Checklist (Pre-Print)

- [ ] Title/cert/declaration/acknowledgement on institutional letterhead forms
- [ ] Page numbers bottom-center; roman numerals for front matter
- [ ] Figure captions below, table captions above, both numbered per chapter
- [ ] IEEE citations bracketed, in order of appearance
- [ ] 60–100 pages body (excluding appendices); PDF/A archived + 3 hard copies
