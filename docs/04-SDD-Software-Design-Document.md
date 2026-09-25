# Document 04 — Software Design Document (SDD)

## Technical Blueprint for OMC: Offline Mesh Communication

| Field | Detail |
|---|---|
| **Document ID** | OMC-SDD-1.0 |
| **Version** | 1.0 (matches frozen protocol OMC/1.0) |
| **Date** | September 2026 |
| **Authors** | Shreyas Pawar, Veer Shah, Sayal Shah, Dhanashri Adawade |
| **Approver** | Internal Guide |
| **Status** | Approved |

> Normative companion to the SRS (Doc 03). If this document and code disagree, file a change request per RPD §10 — do not silently drift.

---

## 1. Introduction

### 1.1 Purpose

Define the system architecture, component responsibilities, data model, data flows, wire protocol, internal APIs, state machines, and deployment view so four developers can implement in parallel against stable contracts.

### 1.2 Design Goals (in priority order)

1. **Correctness under churn:** nodes join/leave/move; messages must terminate (no loops), deduplicate, and survive gaps.
2. **Simplicity over optimality (v1):** flooding + DTN beats AODV complexity for a semester build; AODV is reserved for v2.
3. **Android realism:** foreground service, permission paths, OEM quirks, and battery budget drive structure — not just clean layers.
4. **Testability:** pure routing/DTN logic isolated from Android APIs so it is unit-testable on JVM.

### 1.3 References

- Doc 03 (SRS) for FR/NFR IDs cited throughout as `FR-x`.
- Google Nearby Connections API docs; Android BLE / Foreground Service guides.
- RFC 3561 (AODV — v2 reference, not v1 behavior).

---

## 2. High-Level Architecture

### 2.1 Layered View

```
+------------------ UI LAYER -------------------+
| Home | Peers | Chat | Diagnostics | Settings |
| Jetpack Compose + ViewModels (StateFlow)      |
+-----------------------+-------------------+
                        | StateFlow / ViewModel
+-----------------------v-------------------+
|              APPLICATION LAYER             |
| MeshManager | MessageRepo | PeerRepo       |
+------+------+------------+--------+--------+
       |      |                     |
+------v---+--v------+  +-----------v---------+
| ROUTING  | DTN     |  | SECURITY (optional) |
| Engine   | STORE   |  | Signer / Verifier   |
| (pure)   | (Room)  |  +-------------------+
+------+---+---+---+
       |       |
+------v-------v-----------------------------+
|             TRANSPORT LAYER                 |
| Nearby P2P_CLUSTER | BLE adv/scan | Codec  |
+--------------------------------------------+
                        |
+-----------------------v-------------------+
|             ANDROID PLATFORM                |
| BLE | Wi-Fi Direct | Foreground Service      |
+--------------------------------------------+
```

**Layering rules:**

- UI never touches transport directly — only via `MeshApi` + repositories.
- Routing/DTN logic is pure Kotlin (no `Context`) — inject `StorageApi` and `TransportSender` interfaces.
- Transport owns all Android API calls and maps them to `onPeerFound / onConnected / onPayload` callbacks upward.

### 2.2 Package Map (Normative)

```
com.example.omc/
  mesh/         MeshManager, NeighborTable, SeenPacketCache
  discovery/    DiscoveryManager (advertise/scan wrapper)
  connection/   ConnectionManager (lifecycle + HELLO)
  protocol/     MeshPacket, PacketCodec, PacketType, Flags
  routing/      RoutingEngine (flooding + TTL + dedup)
  dtn/          DtnStore, FlushScheduler
  ack/          AckManager (matching + retry)
  heartbeat/    HeartbeatManager (keepalive + dead detection)
  security/     Signer, Verifier (optional)
  storage/      AppDatabase, NodeDao, NeighborDao, MessageDao, AckDao, Entities
  service/      MeshForegroundService, BootReceiver
  ui/           screens + viewmodels + navigation
```

---

## 3. Component Design

### 3.1 Component Diagram

```
+------------+     +------------+     +------------+
| Discovery  +-----> Connection +-----> Neighbor   |
| Manager    |     | Manager    |     | Table      |
+------------+     +-----+------+     +-----+------+
                         |                  |
                         v                  v
                  +------------+     +------------+
                  | Packet     |<--->| Routing    |
                  | Codec      |     | Engine     |
                  +-----+------+     +-----+------+
                        |                  |
                        v                  v
                  +------------+     +------------+
                  | DTN Store  |<--->| ACK Manager|
                  | (Room)     |     +------------+
                  +-----+------+
                        |
                        v
                  +------------+
                  | Heartbeat  |
                  | Manager    |
                  +-----+------+
                        |
                        v
              +--------------------+
              | MeshManager (facade)|
              +---------+----------+
                        |
                        v
                  +------------+
                  | UI Layer   |
                  +------------+
```

### 3.2 Key Classes and Responsibilities

| Class | Responsibility | Key State |
|---|---|---|
| `MeshManager` | Facade: start/stop, send/broadcast, exposes Flows; wires all managers | `isRunning`, `nodeId` |
| `DiscoveryManager` | Wraps `startAdvertising/startDiscovery`; emits `DiscoveredPeer` | advertising/discovering flags |
| `ConnectionManager` | `requestConnection/accept`, HELLO exchange, link callbacks | `endpointId ↔ nodeId` map |
| `NeighborTable` | Thread-safe active-peer set + `lastSeen` + status | `Map<nodeId, NeighborEntry>` |
| `RoutingEngine` | Pure decision: deliver / forward / store (see §7.4) | ref to `SeenPacketCache` |
| `SeenPacketCache` | Bounded LRU (default 1000) of `packet_id` | `LinkedHashMap` w/ access order |
| `DtnStore` | Pending-queue CRUD + expiry + FIFO-per-destination query | Room DAO |
| `AckManager` | Generates ACKs; matches inbound ACKs; schedules retries w/ backoff | `Map<packetId, PendingAck>` |
| `HeartbeatManager` | Periodic HEARTBEAT send (4 s) + dead sweep (1 s tick, 12 s threshold) | `lastSeen` per neighbor |
| `PacketCodec` | JSON serialize/validate/deserialize; enforces size bounds | Gson instance |
| `Signer / Verifier` | Optional sign/verify (off by default) | key handle |
| `MeshForegroundService` | Owns `MeshManager` lifetime; notification; auto-restart | service state |
| `MessageRepository` | UI-facing message Flows + send path | Room + MeshManager |

### 3.3 Concurrency Model

- **Single-threaded routing core:** all `onReceive` decisions run on a dedicated `Dispatchers.Default` single-thread dispatcher (mailbox) to avoid races on `SeenPacketCache` and neighbor snapshots.
- **Coroutines + Flow:** repositories expose `StateFlow<List<…>>`; UI collects lifecycle-aware.
- **Transport callbacks** (Nearby runs on binder threads) immediately hop to the routing dispatcher via `scope.launch(routingDispatcher)`.
- **No `GlobalScope`.** Every manager takes a `CoroutineScope` from `MeshManager` so `stop()` cancels everything deterministically.

---

## 4. Data Model — ER Design and Room Schema

### 4.1 ER Diagram (Text)

```
+-------------------+         +-------------------+
| NODE              |         | MESSAGE           |
|-------------------|         |-------------------|
| node_id (PK)      |<---+    | packet_id (PK)    |
| display_name      |    |    | source_id (FK)    |
| public_key (null) |    |    | destination_id    |
| created_at        |    |    | packet_type       |
+-------------------+    |    | payload_json      |
                         |    | status            |
+-------------------+    |    | ttl               |
| NEIGHBOR          |    |    | hop_count         |
|-------------------|    |    | created_at        |
| neighbor_id (PK)  |    |    | expires_at        |
| node_id (FK) -----+    |    | retries           |
| endpoint_id       |    +----+-------------------+
| last_seen         |              | 1
| rssi (null)       |              | N
| status            |    +-------------------+
+-------------------+    | ACK               |
                         |-------------------|
                         | ack_id (PK)       |
                         | packet_id (FK)    |
                         | received_at       |
                         | from_node_id (FK) |
                         +-------------------+
```

- `BROADCAST` is a sentinel `destination_id = "BROADCAST"` (not a row in NODE).
- ACK rows are audit/history; the live pending-ACK map lives in memory (`AckManager`) for retry timing, mirrored to DB on delivery.

### 4.2 Room Entities (Kotlin — Normative Shape)

```kotlin
@Entity(tableName = "nodes")
data class NodeEntity(
    @PrimaryKey val nodeId: String,
    val displayName: String?,
    val publicKey: String?,
    val createdAt: Long
)

@Entity(
    tableName = "neighbors",
    foreignKeys = [ForeignKey(
        entity = NodeEntity::class,
        parentColumns = ["nodeId"], childColumns = ["nodeId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("nodeId")]
)
data class NeighborEntity(
    @PrimaryKey val neighborId: String, // == nodeId in v1
    val nodeId: String,
    val endpointId: String,
    val lastSeen: Long,
    val rssi: Int?,
    val status: String // CONNECTED | DEAD
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val packetId: String,
    val sourceId: String,
    val destinationId: String, // or "BROADCAST"
    val packetType: String,    // DATA | ACK | HELLO | HEARTBEAT
    val payloadJson: String,
    val status: String,        // PENDING|SENT|DELIVERED|FAILED|EXPIRED
    val ttl: Int,
    val hopCount: Int,
    val createdAt: Long,
    val expiresAt: Long,
    val retries: Int
)

@Entity(
    tableName = "acks",
    foreignKeys = [ForeignKey(
        entity = MessageEntity::class,
        parentColumns = ["packetId"], childColumns = ["packetId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("packetId")]
)
data class AckEntity(
    @PrimaryKey val ackId: String,
    val packetId: String,
    val receivedAt: Long,
    val fromNodeId: String
)
```

### 4.3 DAO Contracts (Abridged)

```kotlin
@Dao interface MessageDao {
    @Insert(onConflict = REPLACE) suspend fun upsert(m: MessageEntity)
    @Query("SELECT * FROM messages WHERE status='PENDING' AND destinationId=:dest ORDER BY createdAt ASC")
    suspend fun pendingFor(dest: String): List<MessageEntity>
    @Query("SELECT * FROM messages WHERE status='PENDING' ORDER BY createdAt ASC")
    suspend fun allPending(): List<MessageEntity>
    @Query("UPDATE messages SET status='DELIVERED' WHERE packetId=:id")
    suspend fun markDelivered(id: String)
    @Query("DELETE FROM messages WHERE expiresAt < :now")
    suspend fun purgeExpired(now: Long): Int
}
```

---

## 5. Data Flow Diagrams

### 5.1 Level 0 — Context

```
      +--------+   message   +--------+   packet   +--------+
      | User   +-------------> OMC    +------------> Peer   |
      +--------+             | System |            | Device |
      +--------+   packet    |        |<-----------+        |
      | Peer   +------------->        <------------+        |
      +--------+             +--------+            +--------+
```

### 5.2 Level 1 — Main Processes

```
User -> [1.0 Compose] -> [2.0 Build Packet] -> [3.0 Route Decision]
                                                   |
                        [5.0 Neighbor Table] -> [3.0]
                                                   v
                                          [4.0 Transport] -> Peer Device
                                                                 |
                                                                 v
                                                        [6.0 Inbound Handler]
                                                     +------+------+------+
                                                     v      v             v
                                              [7.0 Deliver] [8.0 Forward] [9.0 Store]
                                                     v      v             v
                                                  UI Log  Neighbors    Room DB
```

### 5.3 Send Sequence (Unicast with ACK)

```
User  UI  MeshManager  RoutingEngine  Transport  Peer
 |     |       |             |            |        |
 |--compose--->|             |            |        |
 |     |--send(dest,text)--->|            |        |
 |     |       |--build pkt->|            |        |
 |     |       |             |-route----->|        |
 |     |       |             |            |-payload>
 |     |       |             |            |<--ACK---
 |     |<------delivered-----|<-----------|        |
```

### 5.4 Multi-Hop Forward (A→B→C→D, TTL 10)

```
Node A -> Node B -> Node C -> Node D
 | DATA     | fwd      | fwd      | deliver
 | ttl=10   | ttl=9    | ttl=8    |
 |<--------- ACK ------+----------+ (relayed back, TTL applies)
```

---

## 6. Wire Protocol — OMC/1.0 (Normative)

### 6.1 Packet Structure (JSON over Nearby BYTES)

```json
{
  "header": {
    "version": 1,
    "packet_id": "550e8400-e29b-41d4-a716-446655440000",
    "packet_type": "DATA",
    "source_id": "node-A-uuid",
    "destination_id": "node-D-uuid",
    "ttl": 10,
    "hop_count": 0,
    "timestamp": 1700000000000,
    "flags": 1
  },
  "payload": {
    "message_text": "I am safe at Gate 3",
    "signature": null,
    "ack_for_packet_id": null
  }
}
```

### 6.2 Packet Types

| Type | Cast | Purpose | ACK? |
|---|---|---|---|
| HELLO | unicast | Post-connect intro (node_id, name, version) | No |
| HEARTBEAT | broadcast-ish (per neighbor) | Keepalive; updates `lastSeen` | No |
| RREQ / RREP | — | **Reserved for v2 AODV**; v1 nodes drop unknown types with counter | — |
| DATA | unicast / BROADCAST | User message | Unicast default yes; broadcast no |
| ACK | unicast | `ack_for_packet_id` + original source as destination | No (never ACK an ACK) |

### 6.3 Flags (bitmask, `header.flags`)

| Bit | Name | Meaning |
|---|---|---|
| 0 | ACK_REQUIRED | Destination must reply ACK (unicast default 1) |
| 1 | ENCRYPTED | Reserved v2 (v1 receivers ignore + log) |
| 2 | SIGNED | `payload.signature` present; verify if enabled |
| 3 | FRAGMENT | Reserved v2 (v1 rejects fragmented flag with counter) |

### 6.4 Validation Rules (Receivers MUST Enforce)

1. `version == 1`, else drop + `counter.unknown_version++`.
2. `packet_type` in known set, else drop + `counter.unknown_type++`.
3. `packet_id` valid UUID, `source_id` non-empty, `ttl` in 0..15, `hop_count` in 0..15.
4. Total decoded bytes ≤ 8 KB (4 KB text + header margin), else drop + `counter.oversize++`.
5. Malformed JSON → drop + `counter.malformed++`. **Never throw to transport thread.**

### 6.5 Routing Algorithm — v1 Flooding (Normative Pseudocode)

```
onReceive(packet, incomingEndpoint):
    if packet.packet_id in seenCache: counter.dup++; return  // duplicate
    seenCache.add(packet.packet_id)

    if packet.destination_id == self OR packet.destination_id == BROADCAST:
        deliverLocally(packet)
        if packet is DATA and ACK_REQUIRED and not BROADCAST:
            sendAck(packet)  // unicast to source_id (routed, TTL applies)
        if BROADCAST and packet.ttl > 1:
            flood(packet, exclude=incomingEndpoint)  // continue broadcast
        return

    // unicast for someone else
    if packet.ttl <= 1: counter.ttl_drop++; return
    packet.ttl -= 1
    packet.hop_count += 1

    nextHops = neighbors(exclude=incomingEndpoint)
    if nextHops.isEmpty():
        dtnStore.save(packet)  // FR-5
    else:
        for n in nextHops: transport.send(n, packet)
```

Termination argument (for viva): TTL strictly decreases per relay and is bounded (≤15) → finite steps; dedup cache additionally suppresses re-floods; split-horizon removes 1-hop ping-pong. Hence no infinite loop in any topology.

### 6.6 DTN Algorithm (Normative)

```
onReceiveUnroutable(packet): dtnStore.save(packet)  // status PENDING

onNeighborAdded(node):
    for p in dtnStore.pendingFor(node.destination): transport.send(node, p)
    periodicSweep(every 30s): purgeExpired(); retry allPending() if route exists

onAckReceived(ack):
    dtnStore.markDelivered(ack.ack_for_packet_id)
    ackManager.cancelRetry(ack.ack_for_packet_id)
```

### 6.7 Heartbeat and Failure Detection (Normative)

```
every T_hb (default 4s): for n in neighbors: send(HEARTBEAT)
every 1s:
    for n in neighbors:
        if now - n.lastSeen > T_dead (default 12s):
            markDead(n); emit NeighborLost(n)
            requeue pending via n -> dtnStore (FR-7.3)

Constraint enforced in Settings: T_dead >= 2 * T_hb.
```

---

## 7. Internal API Contracts (Normative for Parallel Work)

```kotlin
interface MeshApi {
    fun start()
    fun stop()
    fun sendMessage(destinationId: String, text: String): String // returns packet_id
    fun broadcastMessage(text: String): String
    fun observeMessages(): Flow<List<MessageEntity>>
    fun observeNeighbors(): Flow<List<NeighborEntity>>
    fun getNodeId(): String
}

interface DiscoveryApi {
    fun startAdvertising(); fun startDiscovery(); fun stop()
    fun observePeers(): Flow<List<DiscoveredPeer>>
}

interface RoutingApi {
    fun route(packet: MeshPacket, incomingEndpoint: String?)
    fun onNeighborAdded(nodeId: String, endpointId: String)
    fun onNeighborRemoved(nodeId: String)
}

interface StorageApi {
    suspend fun saveMessage(m: MessageEntity)
    suspend fun pendingFor(dest: String): List<MessageEntity>
    suspend fun allPending(): List<MessageEntity>
    suspend fun markDelivered(packetId: String)
    suspend fun purgeExpired(now: Long): Int
}

interface TransportSender {
    fun sendTo(endpointId: String, packet: MeshPacket)
    fun broadcast(packet: MeshPacket)
}
```

**External APIs consumed:** `ConnectionsClient.startAdvertising/startDiscovery/requestConnection/acceptConnection/sendPayload`, `PayloadCallback`, `BluetoothAdapter` state checks, Room, `Service.startForeground`.

---

## 8. State Machines

### 8.1 Link State

```
DISCONNECTED -> DISCOVERED -> CONNECTING -> CONNECTED -> DEAD
     ^                                                    |
     +------------------(sweep/retry)--------------------+
```

### 8.2 Message State

```
CREATED -> PENDING -> SENT -> DELIVERED (-> ACKED audit)
              |         |         |
              v         v         v
           EXPIRED    FAILED    RETRY (bounded, backoff) -> SENT
```

### 8.3 Neighbor Liveness

```
ALIVE --(heartbeat received: lastSeen=now)--> ALIVE
ALIVE --(now-lastSeen > T_dead)--> DEAD --(HELLO/reconnect)--> ALIVE
```

---

## 9. Deployment View

- Single APK, no backend, no cloud. Room DB is per-device.
- `MeshForegroundService` owns `MeshManager` lifetime; notification shows peer count + pending count with Stop action.
- `BootReceiver` (optional, behind setting) re-starts service after reboot.
- Release: R8 minify, versionCode bump, signed bundle/APK; Crashlytics opt-in.

---

## 10. Design Decisions and Rationale (ADR Summary)

| Decision | Chosen | Rejected Alternative | Rationale |
|---|---|---|---|
| Transport | Nearby Connections P2P_CLUSTER | Raw BLE + Wi-Fi Direct sockets | 1 API for discovery+transfer; semester-feasible |
| Routing v1 | Flooding + TTL + dedup | AODV | Robust under churn; no route-table convergence wait; AODV deferred to v2 |
| Serialization | JSON/Gson | Protobuf | Debuggability beats bytes in v1 |
| Storage | Room | Raw SQLite / DataStore | Type safety + Flow + migrations |
| Background | Foreground service | WorkManager/periodic | Mesh needs continuous radio, not periodic jobs |
| Concurrency | Single routing dispatcher + coroutines | Synchronized-everywhere | Deterministic ordering, fewer deadlocks |

---

## 11. Error Handling and Observability

- Transport thread never throws to platform: all decode/route wrapped; counters (`malformed`, `dup`, `ttl_drop`, `unknown_*`) exposed on Diagnostics.
- Bounded structures everywhere: seen-cache LRU 1000, pending FIFO + expiry, retry ≤ 3 with backoff (10 s base).
- Log tags: `OMC-MESH`, `OMC-ROUTE`, `OMC-DTN`, `OMC-ACK`, `OMC-HB` — shareable via in-app export (FR-10.3).

*Next document: 05 — Test Plan & Test Cases Report.*
