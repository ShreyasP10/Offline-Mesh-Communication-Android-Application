package com.example.omc.ui;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.omc.R;
import com.example.omc.mesh.MeshLogger;

import java.util.ArrayList;
import java.util.List;

public class LogAdapter extends RecyclerView.Adapter<LogAdapter.LogViewHolder> {

    private final List<MeshLogger.LogEntry> logEntries = new ArrayList<>();

    public void setLogs(List<MeshLogger.LogEntry> logs) {
        logEntries.clear();
        if (logs != null) {
            logEntries.addAll(logs);
        }
        notifyDataSetChanged();
    }

    public void addLog(MeshLogger.LogEntry entry) {
        if (entry != null) {
            logEntries.add(entry);
            notifyItemInserted(logEntries.size() - 1);
        }
    }

    public void clear() {
        logEntries.clear();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public LogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_log, parent, false);
        return new LogViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LogViewHolder holder, int position) {
        MeshLogger.LogEntry entry = logEntries.get(position);
        holder.bind(entry);
    }

    @Override
    public int getItemCount() {
        return logEntries.size();
    }

    static class LogViewHolder extends RecyclerView.ViewHolder {
        private final TextView logTag;
        private final TextView logTimestamp;
        private final TextView logMessage;

        LogViewHolder(@NonNull View itemView) {
            super(itemView);
            logTag = itemView.findViewById(R.id.logTag);
            logTimestamp = itemView.findViewById(R.id.logTimestamp);
            logMessage = itemView.findViewById(R.id.logMessage);
        }

        void bind(MeshLogger.LogEntry entry) {
            logTag.setText(entry.getTag());
            logTimestamp.setText(entry.getTimestamp());
            logMessage.setText(entry.getMessage());

            if ("E".equals(entry.getLevel())) {
                logTag.setTextColor(0xFFEF4444);
                logMessage.setTextColor(0xFFDC2626);
            } else if ("I".equals(entry.getLevel())) {
                logTag.setTextColor(0xFF3B82F6);
                logMessage.setTextColor(0xFF1E293B);
            } else {
                logTag.setTextColor(0xFF64748B);
                logMessage.setTextColor(0xFF1E293B);
            }
        }
    }
}
