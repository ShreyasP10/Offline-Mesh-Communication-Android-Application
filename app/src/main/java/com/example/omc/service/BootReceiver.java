package com.example.omc.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.omc.mesh.MeshConfig;
import com.example.omc.storage.DatabaseHelper;

/**
 * Auto-starts OMC Mesh upon device boot if enabled in settings per FR-9.2.
 */
public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = MeshConfig.LOG_MESH;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            return;
        }

        DatabaseHelper dbHelper = DatabaseHelper.getInstance(context);
        boolean autoStart = Boolean.parseBoolean(dbHelper.getSetting("boot_autostart", "false"));

        if (autoStart) {
            Log.i(TAG, "Device booted; auto-starting OMC Mesh Foreground Service per FR-9.2");
            MeshForegroundService.start(context);
        } else {
            Log.d(TAG, "Device booted; boot auto-start is disabled in settings");
        }
    }
}
