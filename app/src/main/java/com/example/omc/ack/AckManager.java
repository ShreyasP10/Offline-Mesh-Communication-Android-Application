package com.example.omc.ack;

import android.os.Handler;
import android.os.Looper;

import com.example.omc.mesh.MeshConfig;
import com.example.omc.mesh.MeshLogger;
import com.example.omc.protocol.OMCMessage;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages reliable delivery acknowledgements (ACKs) and bounded retries with backoff.
 */
public class AckManager {

    private static final String TAG = MeshConfig.LOG_ACK;
    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_RETRY_DELAY_MS = 6_000L;

    public interface AckListener {
        void onDeliveryConfirmed(String messageId);
        void onDeliveryFailed(String messageId);
        void onRetryRequested(OMCMessage message, int attempt);
    }

    private static class PendingAck {
        final OMCMessage message;
        int retriesAttempted;
        long lastSentTime;
        final Runnable retryRunnable;

        PendingAck(OMCMessage message, Runnable retryRunnable) {
            this.message = message;
            this.retriesAttempted = 0;
            this.lastSentTime = System.currentTimeMillis();
            this.retryRunnable = retryRunnable;
        }
    }

    private final Map<String, PendingAck> pendingAcks = new ConcurrentHashMap<>();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private AckListener listener;

    public void setListener(AckListener listener) {
        this.listener = listener;
    }

    public void trackMessage(OMCMessage message) {
        if (message == null || message.getHeader() == null) return;
        final String messageId = message.getHeader().getMessageId();

        Runnable retryRunnable = new Runnable() {
            @Override
            public void run() {
                handleRetry(messageId);
            }
        };

        PendingAck pendingAck = new PendingAck(message, retryRunnable);
        pendingAcks.put(messageId, pendingAck);

        // Schedule first timeout
        handler.postDelayed(retryRunnable, INITIAL_RETRY_DELAY_MS);
        MeshLogger.log(TAG, "Tracking ACK for message: " + messageId);
    }

    public void onAckReceived(String acknowledgedMessageId) {
        if (acknowledgedMessageId == null) return;

        PendingAck pending = pendingAcks.remove(acknowledgedMessageId);
        if (pending != null) {
            handler.removeCallbacks(pending.retryRunnable);
            MeshLogger.log(TAG, "ACK confirmed for message: " + acknowledgedMessageId);
            if (listener != null) {
                listener.onDeliveryConfirmed(acknowledgedMessageId);
            }
        }
    }

    private void handleRetry(String messageId) {
        PendingAck pending = pendingAcks.get(messageId);
        if (pending == null) return;

        if (pending.retriesAttempted < MAX_RETRIES) {
            pending.retriesAttempted++;
            MeshLogger.log(TAG, "Retrying message " + messageId + " (attempt " + pending.retriesAttempted + "/" + MAX_RETRIES + ")");

            if (listener != null) {
                listener.onRetryRequested(pending.message, pending.retriesAttempted);
            }

            // Exponential backoff
            long delay = INITIAL_RETRY_DELAY_MS * (1L << pending.retriesAttempted);
            handler.postDelayed(pending.retryRunnable, delay);
        } else {
            // Retries exhausted
            pendingAcks.remove(messageId);
            MeshLogger.log(TAG, "Message " + messageId + " delivery failed after max retries", "E");
            if (listener != null) {
                listener.onDeliveryFailed(messageId);
            }
        }
    }

    public void clear() {
        for (PendingAck ack : pendingAcks.values()) {
            handler.removeCallbacks(ack.retryRunnable);
        }
        pendingAcks.clear();
    }
}
