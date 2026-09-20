# Document 02 — Requirements Planning Document (RPD)

## Project Management Plan for OMC: Offline Mesh Communication Android Application

| Field | Detail |
|---|---|
| **Document ID** | OMC-RPD-1.0 |
| **Version** | 1.0 |
| **Date** | ____________ |
| **Authors** | Project Team (Lead + 3 Members) |
| **Approver** | Internal Guide / Project Coordinator |
| **Status** | Approved / Under Review (circle one) |

---

## 1. Purpose and Scope of This Document

This RPD defines **how** the OMC project will be executed, controlled, and delivered. It covers:

- Development methodology and rationale
- Team organization and responsibility matrix
- Resource allocation (human, hardware, software)
- Work Breakdown Structure (WBS)
- 16-week schedule with milestones and Gantt chart
- Risk management plan with register
- Quality assurance plan
- Communication and change-control procedures

Companion documents: `01-Project-Proposal-Synopsis.md` (what/why), `03-SRS` (requirements), `04-SDD` (design).

---

## 2. Development Methodology

### 2.1 Selected Model: Hybrid Agile-Waterfall

| Phase | Model | Rationale |
|---|---|---|
| Phase 0 — Initiation, Requirements, Protocol, Architecture (Wks 1–4) | **Waterfall** (sequential, sign-off gated) | The wire protocol (OMC/1.0) and data model must be **frozen** before parallel coding; otherwise flooding, DTN, and ACK modules diverge |
| Phase 1–4 — Implementation sprints (Wks 5–12) | **Agile/Scrum** (2-week sprints, demo at end) | App features (discovery, routing, UI) benefit from iterative feedback on real devices |
| Phase 5 — Testing + Documentation (Wks 13–16) | **Waterfall gate + Agile fixes** | Test report and dissertation need stable baselines; bug fixes iterate in short cycles |

### 2.2 Why Not Pure Waterfall or Pure Agile?

- **Pure Waterfall** fails because radio behavior on real phones (BLE throttling, OEM kills) cannot be fully predicted on paper — field feedback is essential.
- **Pure Agile** fails because four developers cannot parallelize mesh work without a frozen packet format; changing `packet_id` semantics mid-sprint breaks everyone.
- The hybrid gives **stability where needed (protocol)** and **flexibility where valuable (UI, tuning)**.

### 2.3 Sprint Cadence

- **Sprint length:** 2 weeks (4 implementation sprints: S1–S4 map to Weeks 5–12; see §6).
- **Ceremonies per sprint:**
  - Planning (1 hr, day 1): pull backlog items, assign owners, define demo acceptance.
  - Daily standup (15 min, async allowed): what I did / will do / blockers.
  - Demo (30 min, last day): live on 2+ physical devices, guide invited.
  - Retrospective (30 min): keep / fix / try.
- **Definition of Done (DoD) for every backlog item:**
  1. Code merged via PR with ≥ 1 reviewer approval.
  2. Unit tests written and passing (`./gradlew test`).
  3. Android Lint + Detekt clean (no new warnings).
  4. Demoed on ≥ 2 physical devices (or recorded if remote).
  5. Docs/ADR updated if behavior changed.

### 2.4 Version Control Workflow

- **Branching:** `main` (protected, releasable) ← `develop` ← `feature/<short-name>`.
- **PR rules:** squash-merge; PR template requires: linked backlog ID, devices tested, screenshots/log excerpt.
- **Tagging:** `v0.1-discovery`, `v0.2-p2p`, `v0.3-multihop`, `v0.4-dtn`, `v0.5-ack`, `v1.0-final`.
- **CI (recommended):** GitHub Actions running `assembleDebug + test + lint` on every PR.

---

## 3. Team Organization

### 3.1 Roles and Responsibilities

| Role | Person | Primary Responsibility | Secondary |
|---|---|---|---|
| Project Lead | ____ | Architecture, OMC/1.0 spec, integration, code reviews, guide liaison | Unblock anyone |
| Android Dev 1 — Transport | ____ | `discovery/`, `connection/`, permissions, Nearby integration | Field-test lead |
| Android Dev 2 — Network | ____ | `protocol/`, `mesh/` routing engine, DTN store, ACK, heartbeat | Protocol docs |
| Android Dev 3 — App | ____ | `storage/` (Room), `service/`, `ui/` (Compose), settings, diagnostics | User manual, screenshots |
| Test Engineer (rotating) | ____ (rotate per sprint) | Test plan execution, device matrix, bug triage, metrics | — |
| Internal Guide | ____ | Weekly review, academic compliance, viva prep | — |

> **Rotation note:** The Test Engineer role rotates each sprint so knowledge spreads and no single person becomes a bottleneck.

### 3.2 RACI Matrix (Key Activities)

| Activity | Lead | Dev 1 | Dev 2 | Dev 3 | Guide |
|---|---|---|---|---|---|
| Freeze OMC/1.0 spec | A/R | C | R | C | A |
| Discovery module | A | R | C | I | I |
| Routing + DTN | A | C | R | C | I |
| UI + storage | A | I | C | R | I |
| Test report sign-off | R | R | R | R | A |
| Final dissertation | R | R | R | R | A |
| Viva demo | R | R | R | R | C |

*R = Responsible, A = Accountable, C = Consulted, I = Informed.*

### 3.3 Effort Estimate

- Availability: ~5 hrs/week/member (coursework-adjusted) × 4 members = **20 hrs/week team**.
- Duration: 16 weeks → **~320 person-hours** (conservative) to **~1,280 person-hours** (at 20 hrs/week/member peak during implementation — plan for the conservative number, staff for the peak).
- Buffer: 10% unallocated for defect fixing (Weeks 13–14 explicitly reserved).

---

## 4. Resource Allocation

### 4.1 Human Resources

| Resource | Allocation | Notes |
|---|---|---|
| Development | 60% | Coding + reviews |
| Testing (devices) | 20% | Field sessions need all members + phones together |
| Documentation | 15% | SRS/SDD/report written incrementally, not at the end |
| Meetings/reviews | 5% | Standups + guide review |

### 4.2 Hardware Resources

| Item | Qty | Purpose | Owner / Source |
|---|---|---|---|
| Android phones, API 26–34, mixed OEMs | 5 (min 3) | Multi-node mesh tests; OEM diversity catches battery-killer issues | Team + borrowed |
| Development laptops | 4 | Android Studio builds | Personal |
| Power banks + charging hub | 2+ | Long field sessions | Team |
| Measuring tape / markers | 1 set | Distance-controlled tests (1 m / 5 m / 15 m / 30 m) | Team |
| Optional Wi-Fi router | 1 | Baseline comparison only (never required for OMC) | Lab |

**Device matrix (fill before Week 5):**

| Device ID | Model | Android/API | OEM Skin | BLE | Wi-Fi Direct | Assigned To |
|---|---|---|---|---|---|---|
| D-1 | ____ | __ / API __ | ____ | Y/N | Y/N | ____ |
| D-2 | ____ | __ / API __ | ____ | Y/N | Y/N | ____ |
| D-3 | ____ | __ / API __ | ____ | Y/N | Y/N | ____ |
| D-4 | ____ | __ / API __ | ____ | Y/N | Y/N | ____ |
| D-5 | ____ | __ / API __ | ____ | Y/N | Y/N | ____ |

### 4.3 Software Resources

| Tool | Purpose | Cost |
|---|---|---|
| Android Studio (Hedgehog+) + JDK 17 | IDE + build | Free |
| Git + GitHub | VCS, PRs, CI | Free |
| Trello / Jira / GitHub Projects | Backlog + sprint board | Free tier |
| Draw.io / Excalidraw | Architecture, ER, DFD diagrams | Free |
| Google Docs / LaTeX | SRS/SDD/report collaboration | Free |
| Firebase Crashlytics (optional) | Crash monitoring in field builds | Free tier |
| Battery Historian | Battery measurement | Free / open source |
| GanttProject / MS Project / Excel | Gantt rendering for submission | Free / licensed |

### 4.4 Budget (Indicative — Adjust to Institution)

| Item | Est. Cost (INR) |
|---|---|
| Printing (report × 4 copies) | 2,000–3,000 |
| Transport for outdoor field tests | 500–1,000 |
| Power bank (if purchasing) | 1,500–2,500 |
| Miscellaneous (cables, stationery) | 500 |
| **Total** | **~4,500–7,000** |

Software and devices assumed already available / borrowed; no hardware procurement is required for the core project.

---

## 5. Work Breakdown Structure (WBS)

```
1. Project Initiation
   1.1 Literature survey (DTN, AODV, BLE mesh, existing apps)
   1.2 Problem definition + objectives
   1.3 Synopsis / proposal (Doc 01) + approval
2. Requirements & Design
   2.1 SRS (Doc 03) — functional, non-functional, user stories
   2.2 Protocol spec OMC/1.0 — packet format, routing, DTN, ACK
   2.3 SDD (Doc 04) — architecture, components, ER, DFD, APIs
   2.4 Design review gate (Week 4) — protocol FROZEN
3. Implementation (sprints)
   3.1 Discovery module (advertise/scan, permissions)
   3.2 Connection module (P2P, HELLO, neighbor table)
   3.3 2-node messaging (codec + transport + UI slice)
   3.4 Routing engine (flooding, TTL, dedup cache)
   3.5 DTN store-and-forward (Room, expiry, opportunistic flush)
   3.6 ACK subsystem (matching, retry, status UI)
   3.7 Heartbeat + self-heal (keepalive, dead detection, re-route)
   3.8 Foreground service + settings + diagnostics
4. Testing
   4.1 Unit tests (JUnit/MockK, ≥70% core coverage)
   4.2 Integration tests (module boundaries, Robolectric/Inst.)
   4.3 System + field tests (2→5 nodes, distances, kill/rejoin)
   4.4 Performance + battery measurement
5. Documentation & Delivery
   5.1 Test report (Doc 05)
   5.2 User manual + deployment guide (Doc 06)
   5.3 Final dissertation (Doc 07)
   5.4 Demo (live + recorded) + viva deck
```

**Backlog seed (import into Trello/Jira):** each WBS leaf 3.x becomes 1–3 backlog cards with acceptance criteria from the SRS (e.g., "FR-4.1 flooding delivers across 2 hops with TTL decrement — demo A→B→C").

---

## 6. Schedule — 16-Week Plan with Milestones

### 6.1 Milestone Table

| Week | Dates | Milestone | Deliverable | Reviewer |
|---|---|---|---|---|
| 1 | ____ | Kickoff, literature survey | Synopsis draft (Doc 01) | Guide |
| 2 | ____ | Requirements complete | SRS draft (Doc 03) | Guide |
| 3 | ____ | Protocol + architecture | OMC/1.0 draft + SDD draft (Doc 04) | Team + Guide |
| 4 | ____ | **Design review gate** | Approved SDD; **protocol FROZEN** | Guide + Committee |
| 5–6 | ____ | Sprint 1: Discovery + Connection | Demo 1: two phones see + connect | Guide |
| 7–8 | ____ | Sprint 2: 2-node messaging + **Mid review** | Demo 2: offline 1-hop chat; progress report | Committee |
| 9–10 | ____ | Sprint 3: Flooding + DTN | Demo 3: multi-hop relay + store-and-forward | Guide |
| 11–12 | ____ | Sprint 4: ACK + heartbeat/self-heal + service | Demo 4: ACK ticks + kill/rejoin recovery | Guide |
| 13 | ____ | Integration + regression testing | Test report draft (Doc 05) | Test Engineer |
| 14 | ____ | Field testing + metrics | Latency/throughput/battery results | Team |
| 15 | ____ | Documentation | Dissertation draft (Doc 07) + manual (Doc 06) | Guide |
| 16 | ____ | **Final review + viva demo** | Dissertation, APK, video, deck | Committee |

### 6.2 Gantt Chart (Text — Redraw in GanttProject/Excel for Submission)

```
Task                     W1 W2 W3 W4 W5 W6 W7 W8 W9 W10 W11 W12 W13 W14 W15 W16
Literature Survey        ██ ██
SRS                      ██ ██ ██
Protocol & SDD              ██ ██ ██
Design Review Gate                ██
Discovery Module                     ██ ██
Connection + 1-hopMsg                   ██ ██ ██
Mid Review                                         ██
Flooding/Routing                                   ██ ██
DTN Store-Forward                                     ██ ██
ACK + Heartbeat/SelfHeal                                 ██ ██ ██
Foreground Svc + Polish                                       ██ ██ ██ ██
Integration Testing                                                  ██ ██
Field Testing + Metrics                                                   ██ ██
Manual + Dissertation                                                        ██ ██
Final Review & Demo                                                                 ██
```

**Dependencies (critical path):** Protocol freeze (W4) → Discovery (W5) → Connection (W6) → 1-hop msg (W7–8) → Flooding (W9) → DTN (W10) → ACK/self-heal (W11–12) → Testing (W13–14). UI/storage/service parallelize off the critical path after W4. Any slip in W4 freeze propagates — protect it.

### 6.3 Sprint Backlog Snapshot (Example — Adapt)

| Sprint | Goal | Cards (Acceptance) |
|---|---|---|
| S1 (W5–6) | See + connect | Nearby advertise/scan; peer list; request/accept; HELLO; TC-D-01, TC-C-01 pass |
| S2 (W7–8) | 1-hop chat | Packet codec; send/receive ≤500 ms; persist; status PENDING; TC-M-01–04 pass |
| S3 (W9–10) | Multi-hop + DTN | Flooding + TTL + dedup; 2–3 hop delivery; store/flush/expiry; TC-R-01–05, TC-N-01–02 pass |
| S4 (W11–12) | Reliable + resilient | ACK + retries; heartbeat 4 s / dead 12 s; re-route; foreground svc; TC-A-01, TC-H-01–03 pass |

---

## 7. Risk Management

### 7.1 Risk Register

| ID | Risk | Prob. | Impact | Exposure | Mitigation | Contingency | Owner |
|---|---|---|---|---|---|---|---|
| R-1 | Android background BLE throttling / kills | H | H | **High** | Foreground service + persistent notification from S1; test on real OEMs early | Whitelist guide in manual; adaptive scan intervals | Dev 1 |
| R-2 | OEM battery killers (Xiaomi/Oppo/Vivo) | H | M | High | Borrow diverse OEMs; document exemption steps | Demo on Pixel/Samsung if needed; note limitation | Test Eng. |
| R-3 | Nearby Connections limits (bandwidth, concurrent peers) | M | H | High | Bound neighbors (≥10 target); measure throughput early (S2) | Fall back to raw BLE + Wi-Fi Direct P2P; reduce payload | Lead |
| R-4 | Scope creep (E2EE, voice, groups) | M | H | High | Freeze protocol W4; change-control board (Lead + Guide) | Defer to Future Scope with written rationale | Lead |
| R-5 | Device unavailability (< 3 phones) | M | H | High | Confirm device matrix W1; borrow pool | Emulators for UI + 2-phone minimum for mesh claims | Lead |
| R-6 | Team member unavailability | M | M | Med | Pair programming; docs in repo; no single-owner modules | Reassign per RACI; descope polish, never core | Lead |
| R-7 | congested-RF field test noise | M | M | Med | Test at multiple times/places; log RF conditions | Report ranges, not single numbers | Test Eng. |
| R-8 | Play Store policy (if publishing) | L | M | Low | Declare all permissions; foreground-service types | Sideload APK for viva (no store dependency) | Dev 3 |

### 7.2 Risk Review Cadence

- Re-score top 3 risks in each retrospective; update this register.
- Any **High-exposure** risk triggers a same-week spike (time-boxed investigation) before it blocks a sprint goal.

---

## 8. Quality Plan

| Practice | Standard | Enforcement |
|---|---|---|
| Code review | Every PR ≥ 1 approval; checklist (null-safety, coroutine scope, permission paths) | Branch protection |
| Static analysis | Android Lint: 0 new warnings; Detekt: complexity thresholds | CI gate |
| Unit coverage | ≥ 70% on `protocol/`, `mesh/`, `storage/` (JaCoCo) | CI badge; sprint DoD |
| Device testing | Every demo on ≥ 2 physical devices; field matrix (distances × node counts) | Demo sign-off sheet |
| Documentation | ADR for every protocol/behavior decision; docs updated in same PR | PR template checkbox |
| Defect SLA | Critical 24 h / High 48 h / Medium 1 wk / Low backlog | Triage in standup |
| Guide review | Weekly 30 min; written action items | Shared notes doc |

---

## 9. Communication Plan

| Activity | Frequency | Channel | Participants | Artifact |
|---|---|---|---|---|
| Standup | Daily (async OK) | WhatsApp/Discord | Team | Blockers list |
| Sprint planning | Biweekly | In person | Team | Sprint backlog |
| Sprint demo | Biweekly | In person + devices | Team + Guide | Demo recording |
| Guide review | Weekly | In person / video | Lead + Guide | Action items |
| Committee reviews | W4, W8, W16 | Formal | All | Signed forms |
| Risk/issue escalation | As needed | Direct to Lead → Guide | — | Updated register |

---

## 10. Change Control Procedure

**Frozen after Week 4:** `packet format`, `header fields`, `TTL semantics`, `ACK behavior`, `DB schema (breaking changes)`.

1. Requester files a one-page Change Request (what, why, impact on schedule/tests/docs).
2. Lead produces impact analysis within 48 h (modules, tests, docs affected).
3. Guide approves/rejects in weekly review; committee approval if scope-level.
4. On approval: bump spec version (OMC/1.0 → 1.1), update SRS/SDD striking through old text, add migration note, re-run affected tests.

**Rule of thumb:** UI and tuning changes are cheap — approve fast. Protocol and schema changes are expensive — default to **defer to v2** unless a core objective is unachievable without them.

---

## 11. Acceptance and Sign-Off

| Gate | Criteria | Signer |
|---|---|---|
| Design gate (W4) | SRS + OMC/1.0 + SDD approved; device matrix confirmed | Guide |
| Mid review (W8) | 1-hop offline chat demo on 2 phones; progress report | Committee |
| Pre-final (W14) | Multi-hop + DTN + ACK demo; test report with metrics | Guide |
| Final (W16) | Dissertation + APK + video + viva demo | Committee |

---

## 12. Appendices

### A. PR Template (Copy into `.github/pull_request_template.md`)

```markdown
## Linked backlog / FR
## What changed
## Devices tested (model + API)
## Test IDs passed (e.g., TC-M-01)
## Screenshots / log excerpt
## Docs updated? (Y/N + file)
```

### B. Demo Sign-Off Sheet (per sprint)

| Sprint | Date | Devices | Scenarios shown | Pass/Fail | Guide signature |
|---|---|---|---|---|---|
| | | | | | |

### C. Meeting Notes Log

| Date | Attendees | Decisions | Action items (owner + due) |
|---|---|---|---|
| | | | |

*Next document: 03 — Software Requirements Specification (SRS).*
