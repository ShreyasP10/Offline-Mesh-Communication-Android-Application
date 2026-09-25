package com.example.omc.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.omc.R;
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
        exportLogsButton = findViewById(R.id.exportLogsButton);
        clearLogsButton = findViewById(R.id.clearLogsButton);

        backButton.setOnClickListener(v -> finish());

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
        diagMeshStats.setText("Peers Connected: " + peers + " | Routes: " + routes + " | DTN Pending: " + pending);

        int dedup = meshManager.getSeenCacheSize();
        int ttlDrops = meshManager.getTtlDropsCount();
        int dupDrops = meshManager.getDupDropsCount();
        diagCounters.setText("Dedup LRU: " + dedup + " | TTL Drops: " + ttlDrops + " | Dup Drops: " + dupDrops);
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        MeshLogger.removeListener(logListener);
    }
}
