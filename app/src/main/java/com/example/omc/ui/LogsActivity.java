package com.example.omc.ui;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.omc.R;
import com.example.omc.mesh.MeshLogger;

public class LogsActivity extends AppCompatActivity {

    private TextView logStatusText;
    private RecyclerView logsRecyclerView;
    private Button clearLogsButton;
    private LogAdapter logAdapter;

    private final MeshLogger.LogListener logListener = new MeshLogger.LogListener() {
        @Override
        public void onNewLog(MeshLogger.LogEntry entry) {
            logAdapter.addLog(entry);
            logsRecyclerView.smoothScrollToPosition(logAdapter.getItemCount() - 1);
            updateStatusText();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logs);

        logStatusText = findViewById(R.id.logStatusText);
        logsRecyclerView = findViewById(R.id.logsRecyclerView);
        clearLogsButton = findViewById(R.id.clearLogsButton);

        logAdapter = new LogAdapter();
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        logsRecyclerView.setLayoutManager(layoutManager);
        logsRecyclerView.setAdapter(logAdapter);

        logAdapter.setLogs(MeshLogger.getLogs());
        updateStatusText();

        clearLogsButton.setOnClickListener(v -> {
            MeshLogger.clearLogs();
            logAdapter.clear();
            updateStatusText();
        });

        MeshLogger.addListener(logListener);
    }

    private void updateStatusText() {
        logStatusText.setText("System activity (" + logAdapter.getItemCount() + " events captured)");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        MeshLogger.removeListener(logListener);
    }
}
