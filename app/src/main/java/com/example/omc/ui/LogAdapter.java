package com.example.omc.ui;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

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
                .inflate(android.R.layout.simple_list_item_2, parent, false);
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
        private final TextView text1;
        private final TextView text2;

        LogViewHolder(@NonNull View itemView) {
            super(itemView);
            text1 = itemView.findViewById(android.R.id.text1);
            text2 = itemView.findViewById(android.R.id.text2);
            text1.setTextSize(13);
            text2.setTextSize(11);
        }

        void bind(MeshLogger.LogEntry entry) {
            text1.setText("[" + entry.getTag() + "] " + entry.getMessage());
            text2.setText(entry.getTimestamp());

            if ("E".equals(entry.getLevel())) {
                text1.setTextColor(Color.RED);
            } else if ("I".equals(entry.getLevel())) {
                text1.setTextColor(0xFF1976D2);
            } else {
                text1.setTextColor(0xFF333333);
            }
        }
    }
}
