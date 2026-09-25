package com.example.omc.mesh;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

public class MeshLogger {

    public static class LogEntry {
        private final String timestamp;
        private final String tag;
        private final String message;
        private final String level;

        public LogEntry(String tag, String message, String level) {
            this.timestamp = new SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(new Date());
            this.tag = tag;
            this.message = message;
            this.level = level;
        }

        public String getTimestamp() {
            return timestamp;
        }

        public String getTag() {
            return tag;
        }

        public String getMessage() {
            return message;
        }

        public String getLevel() {
            return level;
        }

        public String getFormatted() {
            return "[" + timestamp + "] [" + tag + "] " + message;
        }
    }

    public interface LogListener {
        void onNewLog(LogEntry entry);
    }

    private static final int MAX_LOGS = 250;
    private static final LinkedList<LogEntry> logs = new LinkedList<>();
    private static final List<LogListener> listeners = new ArrayList<>();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public static synchronized void log(String tag, String message) {
        log(tag, message, "D");
    }

    public static synchronized void log(String tag, String message, String level) {
        if ("E".equals(level)) {
            Log.e(tag, message);
        } else if ("I".equals(level)) {
            Log.i(tag, message);
        } else {
            Log.d(tag, message);
        }

        LogEntry entry = new LogEntry(tag, message, level);
        if (logs.size() >= MAX_LOGS) {
            logs.removeFirst();
        }
        logs.add(entry);

        mainHandler.post(() -> {
            synchronized (MeshLogger.class) {
                for (LogListener l : listeners) {
                    try {
                        l.onNewLog(entry);
                    } catch (Exception ignored) {
                    }
                }
            }
        });
    }

    public static synchronized List<LogEntry> getLogs() {
        return new ArrayList<>(logs);
    }

    public static synchronized void clearLogs() {
        logs.clear();
    }

    public static synchronized void addListener(LogListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public static synchronized void removeListener(LogListener listener) {
        listeners.remove(listener);
    }
}
