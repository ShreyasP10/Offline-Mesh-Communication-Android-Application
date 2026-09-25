# Offline Mesh Communication (OMC) — v1.0.0 Release

**OMC (Offline Mesh Communication)** is a decentralized, zero-infrastructure peer-to-peer messaging application for Android devices. It enables off-grid communication during disasters, network outages, remote expeditions, and emergency scenarios without internet, cellular towers, or Wi-Fi routers.

---

### 📦 Release Assets
- **`OMC-Mesh-v1.0.apk`** (~5.38 MB): Production-optimized, signed Release APK with R8/ProGuard optimizations. Ready to install on any Android phone (Android 8.0+ / API 26+).
- **`OMC-Mesh-v1.0-debug.apk`** (~7.71 MB): Signed Debug APK with full logcat tracing enabled.

---

### 🚀 Key Features Included in v1.0.0

- 🔍 **Infrastructure-Free Peer Discovery (FR-1)**:
  - BLE & Nearby Connections (`P2P_CLUSTER`) discovery under 10 seconds.
  - Adaptive battery conservation: high-performance when active, low-power duty cycle when screen is off.
- 🔗 **Automatic Link Management (FR-2)**:
  - Frictionless connection establishment and bidirectional `HELLO` handshake.
- 💬 **Reliable P2P & Broadcast Messaging (FR-3)**:
  - 1-to-1 direct messaging and multi-peer broadcast channels (up to 4 KB payload).
  - Clear real-time delivery status ticks: ⏳ Pending, ➤ Sent, ✓ Delivered, ✗ Failed.
- 🌐 **Flooding Multi-Hop Routing (FR-4)**:
  - Dynamic multi-hop relay with configurable TTL (1–15 hops, default 10).
  - Shortest-path routing table with duplicate packet suppression via 1,000-entry bounded LRU cache.
  - Split-horizon loop avoidance preventing packet bounce-back.
- 📦 **Delay-Tolerant Store-and-Forward (DTN) (FR-5)**:
  - Persistent SQLite message store for unroutable packets.
  - Automatic opportunistic flushing upon peer reconnection in strict FIFO causal order.
  - Configurable message expiry (1–72 hours, default 24h).
- ✅ **End-to-End Delivery Acknowledgements (FR-6)**:
  - Reliable ACKs with exponential backoff and up to 3 automated retries.
  - Multi-hop ACK relaying without intermediate consumption.
- 💓 **Self-Healing Mesh Topology (FR-7)**:
  - 4-second periodic heartbeats and 12-second dead-neighbor detection ($T_{dead} \ge 2 \times T_{hb}$).
  - Automatic neighbor pruning and dynamic packet re-routing.
- 🔒 **Cryptographic Message Integrity & Signing (FR-8)**:
  - Optional HMAC-SHA256 message signing with device keys.
  - Constant-time verification preventing timing attacks; tampered packets are automatically rejected.
  - Zero internet connectivity: all message traffic strictly confined to local radios.
- ⚙️ **Foreground Service & System Integration (FR-9)**:
  - Android 14 (API 34) compatible foreground service (`FOREGROUND_SERVICE_CONNECTED_DEVICE`).
  - Persistent notification with live peer count and 1-tap "Stop" action.
  - Optional auto-start on boot (`BOOT_COMPLETED`).
- 🩺 **Diagnostics & Network Topology Visualizer (FR-10)**:
  - Live diagnostics panel tracking node ID, connected peers, routing metrics, and drop counters.
  - Interactive **Mesh Network Topology** visualizer dialog mapping 1-hop neighbors, multi-hop shortest paths, and DTN pending queue.
  - In-app diagnostic log export via Android Share sheet.

---

### 📱 Supported Platforms & Requirements
- **OS**: Android 8.0 (Oreo / API 26) through Android 14 (UpsideDownCake / API 34).
- **Hardware**: BLE 4.0+ and Wi-Fi Direct capable radio chipset.
- **Root Required**: No.
- **Internet Required**: No (operates natively in Airplane Mode with Bluetooth & Location on).

---

### 🧪 Verification & Test Results
- **Unit Tests**: 100% Passed (ProtocolTest, RoutingTest, SeenPacketCacheTest, SecurityTest, HeartbeatTest).
- **Critical & High Requirements**: 100% Passed (60/60 test cases verified).
- **Release Verification**: R8, ProGuard, Lint Vitals, and APK packaging passed cleanly with 0 errors.

---

### 👥 Contributors
- **Shreyas Pawar** (Project Lead / Architecture)
- **Veer Shah** (Transport & Link Management)
- **Sayal Shah** (Protocol & Routing Engine)
- **Dhanashri Adawade** (Persistence, Service & UI)
