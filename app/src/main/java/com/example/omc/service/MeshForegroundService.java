package com.example.omc.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;

import com.example.omc.MainActivity;
import com.example.omc.R;
import com.example.omc.discovery.Peer;
import com.example.omc.mesh.MeshManager;
import com.example.omc.storage.ChatMessage;

import java.util.List;

public class MeshForegroundService extends Service {

    public static final String ACTION_START = "com.example.omc.action.START_SERVICE";
    public static final String ACTION_STOP = "com.example.omc.action.STOP_SERVICE";

    private static final String CHANNEL_ID = "omc_mesh_channel";
    private static final int NOTIFICATION_ID = 1001;

    private MeshManager meshManager;

    private final android.content.BroadcastReceiver screenReceiver = new android.content.BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) return;
            if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                // Screen off: enter low-power duty cycle to conserve battery (FR-1.4)
                if (meshManager != null) {
                    meshManager.setLowPowerMode(true);
                }
            } else if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
                // Screen on: switch back to aggressive discovery (FR-1.4)
                if (meshManager != null) {
                    meshManager.setLowPowerMode(false);
                }
            }
        }
    };

    private final MeshManager.MeshListener meshListener = new MeshManager.MeshListener() {
        @Override
        public void onMeshStateChanged(boolean running) {
            updateNotification();
            if (!running) {
                stopSelf();
            }
        }

        @Override
        public void onPeersUpdated(List<Peer> peers) {
            updateNotification();
        }

        @Override
        public void onMessageReceived(ChatMessage message) {
            // Keep service notification updated or post notification
        }

        @Override
        public void onMessageStatusChanged(String messageId, String status) {
        }
    };

    public static void start(Context context) {
        Intent intent = new Intent(context, MeshForegroundService.class);
        intent.setAction(ACTION_START);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    public static void stop(Context context) {
        Intent intent = new Intent(context, MeshForegroundService.class);
        intent.setAction(ACTION_STOP);
        context.startService(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        meshManager = MeshManager.getInstance(this);
        meshManager.addListener(meshListener);

        android.content.IntentFilter filter = new android.content.IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(screenReceiver, filter);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            meshManager.stopMesh();
            stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;
        }

        Notification notification = buildNotification("OMC Mesh Active · Initializing...");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
            );
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }

        if (!meshManager.isMeshRunning()) {
            meshManager.startMesh();
        }

        updateNotification();
        return START_STICKY;
    }

    private void updateNotification() {
        if (!meshManager.isMeshRunning()) return;

        int peers = meshManager.getConnectedPeerCount();
        String text = "OMC Mesh Running · " + peers + (peers == 1 ? " peer connected" : " peers connected");
        Notification notification = buildNotification(text);

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, notification);
        }
    }

    private Notification buildNotification(String contentText) {
        Intent openAppIntent = new Intent(this, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(
                this,
                0,
                openAppIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        Intent stopIntent = new Intent(this, MeshForegroundService.class);
        stopIntent.setAction(ACTION_STOP);
        PendingIntent stopPendingIntent = PendingIntent.getService(
                this,
                1,
                stopIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Offline Mesh Communication")
                .setContentText(contentText)
                .setSmallIcon(R.drawable.ic_mesh)
                .setContentIntent(contentIntent)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "OMC Mesh Background Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Keeps offline mesh radio connections alive in the background");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(screenReceiver);
        } catch (Exception ignored) {}
        if (meshManager != null) {
            meshManager.removeListener(meshListener);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
