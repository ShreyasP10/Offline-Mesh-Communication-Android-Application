package com.example.omc.heartbeat;

import android.os.Handler;
import android.os.Looper;

import com.example.omc.discovery.Peer;
import com.example.omc.mesh.MeshConfig;
import com.example.omc.mesh.MeshLogger;

import java.util.List;

/**
 * Manages periodic mesh keepalive heartbeats and dead-node detection.
 */
public class HeartbeatManager {

    private static final String TAG = MeshConfig.LOG_HEARTBEAT;

    public interface HeartbeatListener {
        void onSendHeartbeat();
        void onDeadPeerDetected(Peer peer);
    }

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final HeartbeatListener listener;

    private boolean running = false;

    private final Runnable heartbeatRunnable = new Runnable() {
        @Override
        public void run() {
            if (!running) return;

            if (listener != null) {
                listener.onSendHeartbeat();
            }

            handler.postDelayed(this, MeshConfig.HEARTBEAT_INTERVAL_MS);
        }
    };

    public HeartbeatManager(HeartbeatListener listener) {
        this.listener = listener;
    }

    public void start() {
        if (running) return;
        running = true;
        handler.postDelayed(heartbeatRunnable, MeshConfig.HEARTBEAT_INTERVAL_MS);
        MeshLogger.log(TAG, "HeartbeatManager started (interval: " + MeshConfig.HEARTBEAT_INTERVAL_MS + "ms)");
    }

    public void stop() {
        running = false;
        handler.removeCallbacks(heartbeatRunnable);
        MeshLogger.log(TAG, "HeartbeatManager stopped");
    }

    public void checkDeadPeers(List<Peer> connectedPeers) {
        if (connectedPeers == null || !running) return;

        long now = System.currentTimeMillis();
        for (Peer peer : connectedPeers) {
            if (peer.isConnected() && (now - peer.getLastSeen() > MeshConfig.PEER_TIMEOUT_MS)) {
                MeshLogger.log(TAG, "Peer " + peer.getName() + " timed out (" + (now - peer.getLastSeen()) + "ms)", "E");
                if (listener != null) {
                    listener.onDeadPeerDetected(peer);
                }
            }
        }
    }
}
