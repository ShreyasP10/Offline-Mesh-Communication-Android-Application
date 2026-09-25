package com.example.omc.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.omc.R;
import com.example.omc.discovery.Peer;
import com.example.omc.mesh.MeshConfig;
import com.example.omc.mesh.MeshLogger;
import com.example.omc.mesh.MeshManager;

import java.util.List;

public class LogsActivity extends AppCompatActivity {

    private ImageButton backButton;
    private TextView diagNodeInfo;
    private TextView diagMeshStats;
    private TextView diagCounters;
    private TextView logStatusText;
    private RecyclerView logsRecyclerView;
    private Button viewTopologyButton;
    private Button exportLogsButton;
    private Button clearLogsButton;
    private LogAdapter logAdapter;

    private MeshManager meshManager;

    private final MeshLogger.LogListener logListener = new MeshLogger.LogListener() {
        @Override
        public void onNewLog(MeshLogger.LogEntry entry) {
            logAdapter.addLog(entry);
            logsRecyclerView.smoothScrollToPosition(logAdapter.getItemCount() - 1);
            updateStatusText();
            updateDiagnostics();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logs);

        meshManager = MeshManager.getInstance(this);

        backButton = findViewById(R.id.backButton);
        diagNodeInfo = findViewById(R.id.diagNodeInfo);
        diagMeshStats = findViewById(R.id.diagMeshStats);
        diagCounters = findViewById(R.id.diagCounters);
        logStatusText = findViewById(R.id.logStatusText);
        logsRecyclerView = findViewById(R.id.logsRecyclerView);
        viewTopologyButton = findViewById(R.id.viewTopologyButton);
        exportLogsButton = findViewById(R.id.exportLogsButton);
        clearLogsButton = findViewById(R.id.clearLogsButton);

        backButton.setOnClickListener(v -> finish());
        viewTopologyButton.setOnClickListener(v -> showTopologyDialog());

        logAdapter = new LogAdapter();
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        logsRecyclerView.setLayoutManager(layoutManager);
        logsRecyclerView.setAdapter(logAdapter);

        logAdapter.setLogs(MeshLogger.getLogs());
        updateStatusText();
        updateDiagnostics();

        clearLogsButton.setOnClickListener(v -> {
            MeshLogger.clearLogs();
            logAdapter.clear();
            updateStatusText();
            updateDiagnostics();
        });

        exportLogsButton.setOnClickListener(v -> shareLogs());

        MeshLogger.addListener(logListener);
    }

    private void updateDiagnostics() {
        String nodeName = meshManager.getLocalNode().getNodeName();
        String nodeId = meshManager.getLocalNode().getNodeId();
        String shortId = nodeId.length() > 8 ? nodeId.substring(0, 8) + "..." : nodeId;
        diagNodeInfo.setText("Node: " + nodeName + " (" + shortId + ") | Ver: " + MeshConfig.PROTOCOL_VERSION);

        int peers = meshManager.getConnectedPeerCount();
        int routes = meshManager.getRoutingTableSize();
        int pending = meshManager.getDtnPendingCount();
        long oldestAgeSec = meshManager.getDtnOldestPendingAgeSec();
        String pendingStr = pending + (pending > 0 ? " (" + oldestAgeSec + "s old)" : "");
        diagMeshStats.setText("Peers Connected: " + peers + " | Routes: " + routes + " | DTN Pending: " + pendingStr);

        int dedup = meshManager.getSeenCacheSize();
        int ttlDrops = meshManager.getTtlDropsCount();
        int dupDrops = meshManager.getDupDropsCount();
        int malformedDrops = meshManager.getMalformedDropsCount();
        diagCounters.setText("Dedup LRU: " + dedup + " | TTL Drops: " + ttlDrops + " | Dup: " + dupDrops + " | Malformed: " + malformedDrops);
    }

    private void updateStatusText() {
        logStatusText.setText("System activity (" + logAdapter.getItemCount() + " events captured)");
    }

    private void shareLogs() {
        List<MeshLogger.LogEntry> logs = MeshLogger.getLogs();
        if (logs.isEmpty()) {
            Toast.makeText(this, "No logs to export", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("=== OMC Mesh Diagnostics Log ===\n");
        sb.append("Node ID: ").append(meshManager.getLocalNode().getNodeId()).append("\n");
        sb.append("Node Name: ").append(meshManager.getLocalNode().getNodeName()).append("\n");
        sb.append("Protocol: ").append(MeshConfig.PROTOCOL_VERSION).append("\n");
        sb.append("Connected Peers: ").append(meshManager.getConnectedPeerCount()).append("\n");
        sb.append("DTN Pending: ").append(meshManager.getDtnPendingCount()).append("\n");
        sb.append("TTL Drops: ").append(meshManager.getTtlDropsCount()).append("\n");
        sb.append("Duplicate Drops: ").append(meshManager.getDupDropsCount()).append("\n\n");

        for (MeshLogger.LogEntry entry : logs) {
            sb.append(entry.getFormatted()).append("\n");
        }

        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.setType("text/plain");
        sendIntent.putExtra(Intent.EXTRA_SUBJECT, "OMC Mesh Log - " + meshManager.getLocalNode().getNodeName());
        sendIntent.putExtra(Intent.EXTRA_TEXT, sb.toString());
        startActivity(Intent.createChooser(sendIntent, "Export OMC Logs via"));
    }

    private void showTopologyDialog() {
        StringBuilder sb = new StringBuilder();
        String localNodeName = meshManager.getLocalNode().getNodeName();
        String localNodeId = meshManager.getLocalNode().getNodeId();

        sb.append("📍 LOCAL NODE (Self)\n");
        sb.append("Name: ").append(localNodeName).append("\n");
        sb.append("Node ID: ").append(localNodeId).append("\n");
        sb.append("Status: ").append(meshManager.isMeshRunning() ? "ONLINE (Active)" : "OFFLINE").append("\n");
        sb.append("Protocol: ").append(MeshConfig.PROTOCOL_VERSION).append("\n\n");

        sb.append("🔗 DIRECT 1-HOP NEIGHBORS (").append(meshManager.getConnectedPeerCount()).append(")\n");
        List<Peer> peers = meshManager.getDiscoveredPeers();
        boolean hasConnected = false;
        if (peers != null && !peers.isEmpty()) {
            for (Peer p : peers) {
                if (p.isConnected()) {
                    hasConnected = true;
                    String shortId = p.getNodeId().length() > 8 ? p.getNodeId().substring(0, 8) + "..." : p.getNodeId();
                    sb.append("• ").append(p.getName())
                            .append(" [").append(shortId).append("]\n")
                            .append("  Link: Nearby P2P_CLUSTER (Direct)\n");
                }
            }
        }
        if (!hasConnected) {
            sb.append("  (No direct peers currently connected)\n");
        }

        sb.append("\n🗺️ ROUTING TABLE (Multi-Hop Shortest Paths)\n");
        List<com.example.omc.routing.Route> routes = meshManager.getRoutingTableRoutes();
        if (routes != null && !routes.isEmpty()) {
            for (com.example.omc.routing.Route r : routes) {
                String destId = r.getDestinationId();
                String shortDest = destId.length() > 8 ? destId.substring(0, 8) + "..." : destId;
                sb.append("• Dest: ").append(shortDest)
                        .append(" | Next: ").append(r.getNextHopId())
                        .append(" | Cost: ").append(r.getHopCount()).append(" hop(s)\n");
            }
        } else {
            sb.append("  (No multi-hop routes cached)\n");
        }

        sb.append("\n📦 DTN STORE-AND-FORWARD QUEUE\n");
        int pending = meshManager.getDtnPendingCount();
        if (pending > 0) {
            sb.append("  Pending Messages: ").append(pending)
                    .append(" (Oldest: ").append(meshManager.getDtnOldestPendingAgeSec()).append("s old)\n");
        } else {
            sb.append("  (DTN queue is empty - all packets delivered)\n");
        }

        new AlertDialog.Builder(this)
                .setTitle("Mesh Network Topology")
                .setMessage(sb.toString())
                .setPositiveButton("Close", null)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        MeshLogger.removeListener(logListener);
    }
}
