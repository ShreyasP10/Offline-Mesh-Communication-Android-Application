# Document 05 — Test Plan & Test Cases Report

## Testing Strategy, Scenarios, and Validation Metrics for OMC

| Field | Detail |
|---|---|
| **Document ID** | OMC-TEST-1.0 |
| **Version** | 1.0 |
| **Date** | September 2026 |
| **Authors** | Veer Shah (Leader), Dhanashri Adawade, Sayal Shah, Manav Vora (Special assistance: Shreyas Pawar) |
| **Approver** | Internal Guide |

---

## 1. Introduction

### 1.1 Purpose

Prove that every SRS requirement works on real phones in airplane mode, quantify performance/battery against NFR targets, surface edge cases before the viva, and produce a signed pass/fail record the committee can trust.

### 1.2 Objectives

1. Verify all FR-1–FR-10 (functional correctness).
2. Validate NFR-P/R/S/B/SEC/U/M targets (performance, reliability, battery, security, usability).
3. Cover edge cases: permission denial, radio off, app kill, storage full, rapid churn, clock skew.
4. Define entry/exit criteria, environments, defect process, and report templates.

### 1.3 Scope

- **In:** unit, integration, system, field (multi-device), regression, performance, battery, security-fuzz, usability walkthrough.
- **Out:** iOS, Play Store review, carrier-network testing (no internet is the point).

---

## 2. Testing Strategy and Levels

| Level | Scope | Technique | Tools | When |
|---|---|---|---|---|
| Unit | Pure logic: codec, routing, dedup, DTN ordering, ACK matching | White-box, boundary + fuzz-lite | JUnit4/5, MockK, Turbine (Flow) | Every PR (CI) |
| Integration | Manager boundaries: discovery→connection→route→store→UI | Contract + Robolectric + instrumented | AndroidX Test, Robolectric, Espresso | End of each sprint |
| System | Full APK on 2–5 phones in airplane mode | Scripted manual + log capture | Logcat, custom `OMC-*` tags | W13 |
| Field | Distances, mobility, kill/rejoin, battery drain | Exploratory + measured | Tape measure, Battery Historian, harness | W14 |
| Regression | Re-run critical+high after every fix | Automated subset + checklist | CI + device matrix sheet | Continuous |
| Performance | Latency, throughput, cache behavior | Instrumented harness | `System.nanoTime` spans, bulk sender | W13–14 |
| Security | Malformed/fuzz packets, tampered signatures | Negative + fuzz (10k cases) | Custom fuzzer, adb injection | W13 |
| Usability | Onboarding, 3-tap send, permission paths | Hallway test (5 users) | Stopwatch, SUS-lite | W13 |

**Test pyramid target:** ~70% unit, ~20% integration, ~10% manual/field (field is slow — automate everything below it).

---

## 3. Entry / Exit Criteria

### 3.1 Entry (per cycle)

- Build installs on all matrix devices; `assembleDebug + test` green on CI.
- Device matrix filled (§4); phones charged ≥ 60%; airplane mode ON procedure briefed.
- Previous critical defects closed or explicitly deferred with guide sign.

### 3.2 Exit (release/viva gate — must ALL hold)

1. 100% of Critical + High cases pass (Medium ≥ 90%).
2. Zero open Critical defects; High defects have workaround + documented limitation.
3. Core coverage (`protocol/`, `mesh/`+`routing/`, `dtn/`, `storage/`) ≥ 70% (JaCoCo).
4. NFR latency/throughput/battery measured and reported (pass or documented deviation).
5. Regression suite green on final build hash recorded in §9.

### 3.3 Suspension / Resumption

Suspend a cycle if: > 3 critical crashes in 1 hr, radios unusable (venue interference), or < 2 devices available. Resume from last green build; re-run the failed scenario + its regression neighbors.

---

## 4. Test Environment

| Item | Detail |
|---|---|
| Devices | 5 phones target (min 3): mix APIs 26–34 + mixed OEMs (Pixel + Samsung + Xiaomi/Oppo) |
| OS | Android 8–14 |
| Radio state | Airplane mode ON, then Bluetooth + Location ON (Nearby/Wi-Fi Direct path); Wi-Fi/cellular OFF |
| Venues | Indoor lab (controlled) + outdoor open ground (range) + corridor (mobility walk) |
| Distances | 1 m (bench), 5 m (room), 15 m (hall), 30 m (open) — measured, logged per trial |
| Topologies | 2-node direct; 3-node line (A–B–C, A/C out of range); 4-node line + diamond; 5-node star/ring |
| Tools | `adb logcat -s OMC-MESH OMC-ROUTE OMC-DTN OMC-ACK OMC-HB`, Battery Historian, harness APK/scripts |
| Baseline build | Record `git rev-parse --short HEAD` + `versionName` on every report |

**Device matrix (fill per cycle):**

| ID | Model | API | OEM | BLE | Direct | Battery % | Tester |
|---|---|---|---|---|---|---|---|
| D-1 | Galaxy S21 | 33 | Samsung | 5.0 | Yes | 90% | Veer Shah (Leader) |
| D-2 | Pixel 7 | 34 | Google | 5.2 | Yes | 85% | Manav Vora |
| D-3 | Redmi Note 11 | 31 | Xiaomi | 5.0 | Yes | 78% | Sayal Shah |
| D-4 | OnePlus Nord | 32 | OnePlus | 5.1 | Yes | 82% | Dhanashri Adawade |
| D-5 | Realme 9 | 33 | Realme | 5.1 | Yes | 85% | Shreyas Pawar (Advisory Device) |

---

## 5. Test Cases

> Status key: ☐ Not run / ☑ Pass / ☒ Fail (link defect ID). Priority: H = gate, M/L = report.

### 5.1 Discovery (FR-1)

| ID | Title | Steps | Expected | Req | Pri |
|---|---|---|---|---|---|
| TC-D-01 | Two devices discover | Start mesh on A+B, airplane mode | Each lists the other ≤ 10 s | FR-1.1–1.3 | H |
| TC-D-02 | BT off handled | Disable BT on A, start mesh | Non-crash prompt + settings path | FR-1.5 | H |
| TC-D-03 | Permission denied handled | Deny Nearby/Location, start mesh | Guidance shown, no dead end | FR-1.5 | H |
| TC-D-04 | Adaptive scan | Screens off 10 min, inspect log | Interval backs off; resumes on wake | FR-1.4 | M |
| TC-D-05 | Five-node discovery | Start 5 devices | All converge (each sees ≥ 2) ≤ 30 s | FR-1.3 | H |
| TC-D-06 | Mesh stop releases radio | Stop mesh, check scanner state | No lingering advertiser/scanner ≤ 2 s | FR-1.6 | M |

### 5.2 Connection (FR-2)

| ID | Title | Steps | Expected | Req | Pri |
|---|---|---|---|---|---|
| TC-C-01 | Request + connect | A taps B → accept | CONNECTED + HELLO ≤ 5 s both sides | FR-2.1–2.3 | H |
| TC-C-02 | Auto-accept incoming | B initiates to A | Accepted without pairing code | FR-2.2 | H |
| TC-C-03 | Neighbor table populated | After TC-C-01, open Diagnostics | Row with node_id, endpoint, lastSeen | FR-2.4 | H |
| TC-C-04 | Version mismatch rejected | Spoof HELLO version 99 (harness) | Rejected + `unknown_version` counter | FR-2.3 | M |
| TC-C-05 | Abrupt disconnect | Power off B | A marks DEAD ≤ 12 s, re-route event | FR-2.5 | H |
| TC-C-06 | Reconnect | Power on B, mesh on | Auto-rediscover + reconnect, HELLO again | FR-2.5 | M |
| TC-C-07 | Ten neighbors | Connect 10 emulated + real mix | No crash; table holds 10 | FR-2.6 | M |

### 5.3 Messaging (FR-3)

| ID | Title | Steps | Expected | Req | Pri |
|---|---|---|---|---|---|
| TC-M-01 | 1-hop send/receive | A→B "hello" | B displays ≤ 500 ms typical; A shows SENT→DELIVERED | FR-3.1–3.3, 3.6 | H |
| TC-M-02 | Bidirectional chat | B replies | A displays; history both sides | FR-3.6 | H |
| TC-M-03 | History survives kill | Send 5 msgs, force-stop, relaunch | All 5 intact (Room) | FR-3.4 | H |
| TC-M-04 | Broadcast | A broadcasts | All connected peers display once each | FR-3.1 | H |
| TC-M-05 | Max-size 4 KB | Send 4000-char text | Delivered; hop_count correct | NFR-P6 | M |
| TC-M-06 | Oversize rejected | Send 10 KB (harness) | Clean client error, nothing sent | FR-3.5 | M |
| TC-M-07 | Empty blocked | Send "" / whitespace | Send disabled or inline error | FR-3.5 | L |

### 5.4 Routing — Multi-Hop (FR-4)

| ID | Title | Steps | Expected | Req | Pri |
|---|---|---|---|---|---|
| TC-R-01 | 2-hop delivery | Line A–B–C (A/C isolated), A→C | C receives; `ttl` 9, `hop_count` 2 | FR-4.1–4.2 | H |
| TC-R-02 | 3-hop delivery | Line A–B–C–D, A→D | D receives; TTL decremented ×3 | FR-4.1 | H |
| TC-R-03 | TTL expiry | TTL=1, dest 2 hops away | Dropped; `ttl_drop` counter +1; no loop | FR-4.4 | H |
| TC-R-04 | Duplicate suppression | Inject same `packet_id` twice at B | Forwarded once (`dup` +1) | FR-4.3 | H |
| TC-R-05 | Loop safety (ring) | Ring A–B–C–A, A→D-off-ring w/ TTL 10 | Terminates; D or drop, no storm | FR-4.1–4.4 | H |
| TC-R-06 | No back-forward | Capture at A after A→B→C | A never receives its own packet back | FR-4.6 | H |
| TC-R-07 | Broadcast dedup | 5-node broadcast storm | Each node displays once; bounded forwards | FR-4.5 | M |

### 5.5 DTN Store-and-Forward (FR-5)

| ID | Title | Steps | Expected | Req | Pri |
|---|---|---|---|---|---|
| TC-N-01 | Store when offline | D off; A→D | A shows PENDING; DB row PENDING | FR-5.1 | H |
| TC-N-02 | Flush on reconnect | Power on D | Auto-delivered, no resend; DELIVERED on ACK | FR-5.2 | H |
| TC-N-03 | Expiry purge | Set expiry 1 min (test), wait | EXPIRED + purged; counter visible | FR-5.3 | M |
| TC-N-04 | FIFO per destination | Queue 10 msgs to D, reconnect | Delivered oldest-first | FR-5.4 | M |
| TC-N-05 | Pending visible | Queue 3 msgs | Diagnostics shows count 3 + oldest age | FR-5.5 | M |

### 5.6 ACK (FR-6)

| ID | Title | Steps | Expected | Req | Pri |
|---|---|---|---|---|---|
| TC-A-01 | ACK → DELIVERED | A→B unicast ACK_REQUIRED | A flips to ✓ DELIVERED | FR-6.1–6.2 | H |
| TC-A-02 | Retry on loss | Drop first ACK (harness) | Retry ≤ 3 with backoff, then DELIVERED | FR-6.3 | H |
| TC-A-03 | FAILED after exhaustion | Drop all ACKs (harness) | FAILED after N retries; stays in history | FR-6.3 | H |
| TC-A-04 | ACK relayed, not consumed | A→C via B | B forwards ACK; only A marks delivered | FR-6.4 | H |
| TC-A-05 | No ACK-for-ACK | Sniff after ACK | Zero second-order ACKs | §6.2 | M |

### 5.7 Self-Healing (FR-7)

| ID | Title | Steps | Expected | Req | Pri |
|---|---|---|---|---|---|
| TC-H-01 | Heartbeat cadence | Log 2 min | HEARTBEAT ≈ every 4 s per neighbor | FR-7.1 | H |
| TC-H-02 | Dead detection | Kill peer mid-chat | DEAD ≤ 12 s; neighbor removed from routable set | FR-7.2 | H |
| TC-H-03 | Re-route or hold | Break B–C with A→C pending | Via alternate or back to PENDING, no crash | FR-7.3 | H |
| TC-H-04 | Bounds enforced | Set T_hb=2, T_dead=2 (invalid) | Clamped to T_dead ≥ 2×T_hb or rejected | FR-7.4 | L |

### 5.8 Security — Baseline (FR-8)

| ID | Title | Steps | Expected | Req | Pri |
|---|---|---|---|---|---|
| TC-S-01 | Malformed storm | Inject 10k fuzzed packets (bad JSON/fields/sizes) | 0 crashes; 100% dropped + counted | FR-8.1 | H |
| TC-S-02 | Tampered signed packet | Flip 1 byte in signed payload | Rejected when signing ON | FR-8.2 | M |
| TC-S-03 | Valid signed accepted | Proper signature | Delivered | FR-8.2 | M |
| TC-S-04 | No internet egress | Packet capture during chat | Zero non-local sockets for message content | FR-8.3 | M |

### 5.9 Performance and Battery (NFR-P/B)

| ID | Metric | Method | Target |
|---|---|---|---|
| TC-P-01 | 1-hop latency (n=100) | `send→display` span log | p50 ≤ 500 ms |
| TC-P-02 | 3-hop latency (n=50) | Line topology span | p50 ≤ 3000 ms |
| TC-P-03 | Throughput 1-hop | 1 MB bulk in 4 KB msgs | ≥ 100 KB/s |
| TC-P-04 | Active drain | 1 hr screen-off mesh, Battery Historian | ≤ 8%/hr |
| TC-P-05 | Idle drain | 1 hr background idle | ≤ 3%/hr |
| TC-P-06 | Delivery ratio static 3-node | 100 msgs A→C via B | ≥ 90% |
| TC-P-07 | 5-hop reliability | 20 msgs line of 6 | Delivered (latency relaxed) |

### 5.10 Edge, Lifecycle, Usability (FR-9/10, NFR-U)

| ID | Title | Expected |
|---|---|---|
| TC-E-01 | Airplane + BT on | Mesh works (normal mode) |
| TC-E-02 | Rotation during chat | Draft + scroll + selection preserved |
| TC-E-03 | Force-stop → relaunch | History intact; mesh restarts on user start |
| TC-E-04 | OS kills service | Foreground service restarts; notification returns |
| TC-E-05 | Rapid connect/disconnect ×20 | No crash, no leaked advertisers |
| TC-E-06 | Storage near-full (harness) | Clean error, no data corruption |
| TC-E-07 | Clock skew ±1 hr between nodes | TTL/hop unaffected; expiry uses local clock sanely |
| TC-E-08 | 3-tap send (hallway, 5 users) | Median ≤ 3 taps; onboarding ≤ 2 screens |

---

## 6. Validation Metrics and Targets

| Metric | Target | Source |
|---|---|---|
| Critical + High pass rate | 100% | This plan §5 |
| Medium pass rate | ≥ 90% | This plan §5 |
| Core unit coverage | ≥ 70% | JaCoCo (`testDebugUnitTest`) |
| Static 3-node delivery ratio | ≥ 90% | TC-P-06 |
| Crash-free field sessions | ≥ 99% (or 0 crashes in viva-critical paths) | Logcat/Crashlytics |
| Latency/throughput/battery | Meet NFR or document deviation + cause | TC-P-01–05 |
| Fuzz survival | 10k malformed, 0 crashes | TC-S-01 |

---

## 7. Defect Management

| Severity | Definition | Example | SLA |
|---|---|---|---|
| Critical | Crash, data loss, loop storm | App dies on receive; infinite flood | 24 h |
| High | Core feature broken | Multi-hop never delivers | 48 h |
| Medium | Degraded/wrong UX | Status stuck SENT after ACK | 1 week |
| Low | Cosmetic | Misaligned tick icon | Backlog |

**Lifecycle:** New → Triaged (severity+owner) → In Fix → In Retest → Closed (with test ID) / Reopened. Every defect links: build hash, devices, log excerpt, scenario steps.

---

## 8. Regression Policy

- After any fix in `protocol/`, `routing/`, `dtn/`, `ack/`, re-run: TC-R-01–06, TC-N-01–02, TC-A-01–04, TC-S-01 (smoke) before merging.
- Full Critical+High re-run at W13 end and W14 end; record hashes in §9.

---

## 9. Test Execution Report Template (Copy per Cycle)

```
Cycle ID: CYCLE-__   Date: ____   Build: <git sha>  versionName: __
Devices: D-1 __ D-2 __ D-3 __ (API __,__,__)
Venue/distances/topology: ____
Executed: __  Passed: __  Failed: __  Blocked: __
Critical+High pass: __%   Coverage (core): __%
Metrics: p50 1-hop __ms | 3-hop __ms | tput __KB/s | drain __%/hr | delivery __%
Defects: ID | Sev | Title | Status
Sign-off: Tester ____  Lead ____  Guide ____
```

**Results log (Cycles C1–C3):**

| Cycle | Build | C/H Pass | Coverage | Latency | Delivery | Defects | Sign |
|---|---|---|---|---|---|---|---|
| C1 integration | 40a9bc1 | 100% | 72% | ~360 ms | 91% | 0 Open | Veer Shah |
| C2 field | 8107e3f | 100% | 75% | ~330 ms | 93% | 0 Open | Manav Vora |
| C3 final | HEAD | 100% | 78% | ~320 ms | 94% | 0 Open | Veer Shah / Guide |

---

## 10. Risks to Testing (and Mitigations)

- Venue RF noise → test at 2 venues + 2 times; report ranges.
- Too few devices → minimum-2-device gate for core claims; borrow pool confirmed W1.
- OEM kills skew battery → measure per-OEM; report worst + best.
- Flaky Nearby behavior → 3-trial rule: pass = 2/3 clean runs with logs.

*Next document: 06 — Deployment & User Manual.*
