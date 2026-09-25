package com.example.omc.ui;

import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.omc.R;
import com.example.omc.mesh.MeshManager;
import com.example.omc.storage.DatabaseHelper;

public class SettingActivity extends AppCompatActivity {

    private ImageButton backButton;
    private Switch autoDiscoverySwitch;
    private Switch advertisingSwitch;
    private Switch forwardingSwitch;
    private Switch storeForwardSwitch;
    private Switch bootAutoStartSwitch;
    private Switch signMessagesSwitch;

    private TextView deviceNameText;
    private TextView ttlSettingText;
    private TextView heartbeatSettingText;
    private TextView expirySettingText;
    private Button resetSettingsButton;

    private DatabaseHelper dbHelper;
    private MeshManager meshManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        dbHelper = DatabaseHelper.getInstance(this);
        meshManager = MeshManager.getInstance(this);

        initViews();
        loadSettings();
    }

    private void initViews() {
        backButton = findViewById(R.id.backButton);
        autoDiscoverySwitch = findViewById(R.id.autoDiscoverySwitch);
        advertisingSwitch = findViewById(R.id.advertisingSwitch);
        forwardingSwitch = findViewById(R.id.forwardingSwitch);
        storeForwardSwitch = findViewById(R.id.storeForwardSwitch);
        bootAutoStartSwitch = findViewById(R.id.bootAutoStartSwitch);
        signMessagesSwitch = findViewById(R.id.signMessagesSwitch);

        deviceNameText = findViewById(R.id.deviceNameText);
        ttlSettingText = findViewById(R.id.ttlSettingText);
        heartbeatSettingText = findViewById(R.id.heartbeatSettingText);
        expirySettingText = findViewById(R.id.expirySettingText);
        resetSettingsButton = findViewById(R.id.resetSettingsButton);

        backButton.setOnClickListener(v -> finish());

        deviceNameText.setOnClickListener(v -> showEditDeviceNameDialog());
        ttlSettingText.setOnClickListener(v -> showEditTtlDialog());
        heartbeatSettingText.setOnClickListener(v -> showEditHeartbeatDialog());
        expirySettingText.setOnClickListener(v -> showEditExpiryDialog());

        autoDiscoverySwitch.setOnCheckedChangeListener((btn, isChecked) ->
                dbHelper.saveSetting("auto_discovery", String.valueOf(isChecked)));

        advertisingSwitch.setOnCheckedChangeListener((btn, isChecked) ->
                dbHelper.saveSetting("advertising", String.valueOf(isChecked)));

        forwardingSwitch.setOnCheckedChangeListener((btn, isChecked) ->
                dbHelper.saveSetting("forwarding", String.valueOf(isChecked)));

        storeForwardSwitch.setOnCheckedChangeListener((btn, isChecked) ->
                dbHelper.saveSetting("store_forward", String.valueOf(isChecked)));

        bootAutoStartSwitch.setOnCheckedChangeListener((btn, isChecked) ->
                dbHelper.saveSetting("boot_autostart", String.valueOf(isChecked)));

        signMessagesSwitch.setOnCheckedChangeListener((btn, isChecked) ->
                dbHelper.saveSetting("sign_messages", String.valueOf(isChecked)));

        resetSettingsButton.setOnClickListener(v -> resetSettings());
    }

    private void loadSettings() {
        autoDiscoverySwitch.setChecked(Boolean.parseBoolean(dbHelper.getSetting("auto_discovery", "true")));
        advertisingSwitch.setChecked(Boolean.parseBoolean(dbHelper.getSetting("advertising", "true")));
        forwardingSwitch.setChecked(Boolean.parseBoolean(dbHelper.getSetting("forwarding", "true")));
        storeForwardSwitch.setChecked(Boolean.parseBoolean(dbHelper.getSetting("store_forward", "true")));
        bootAutoStartSwitch.setChecked(Boolean.parseBoolean(dbHelper.getSetting("boot_autostart", "false")));
        signMessagesSwitch.setChecked(Boolean.parseBoolean(dbHelper.getSetting("sign_messages", "false")));

        updateTextDisplays();
    }

    private void updateTextDisplays() {
        String name = meshManager.getLocalNode().getNodeName();
        deviceNameText.setText("Device Name: " + name + " (Tap to edit)");

        String ttl = dbHelper.getSetting("max_ttl", "8");
        ttlSettingText.setText("Max TTL (Hops): " + ttl + " (Tap to edit)");

        String hb = dbHelper.getSetting("heartbeat_interval_sec", "4");
        heartbeatSettingText.setText("Heartbeat Interval: " + hb + " s (Tap to edit)");

        String expiry = dbHelper.getSetting("message_expiry_hours", "24");
        expirySettingText.setText("Message Expiry: " + expiry + " h (Tap to edit)");
    }

    private void showEditDeviceNameDialog() {
        final EditText input = new EditText(this);
        input.setText(meshManager.getLocalNode().getNodeName());
        input.setSelection(input.getText().length());

        new AlertDialog.Builder(this)
                .setTitle("Change Device Name")
                .setMessage("Enter display name for your mesh node:")
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newName = input.getText().toString().trim();
                    if (!newName.isEmpty()) {
                        meshManager.updateDisplayName(newName);
                        updateTextDisplays();
                        Toast.makeText(this, "Device name updated", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showEditTtlDialog() {
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setText(dbHelper.getSetting("max_ttl", "8"));

        new AlertDialog.Builder(this)
                .setTitle("Configure Max TTL (Hops)")
                .setMessage("Enter max hop limit (1 to 15):")
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> {
                    try {
                        int val = Integer.parseInt(input.getText().toString().trim());
                        if (val >= 1 && val <= 15) {
                            dbHelper.saveSetting("max_ttl", String.valueOf(val));
                            updateTextDisplays();
                            Toast.makeText(this, "TTL updated", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "TTL must be between 1 and 15", Toast.LENGTH_SHORT).show();
                        }
                    } catch (NumberFormatException ignored) {}
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showEditHeartbeatDialog() {
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setText(dbHelper.getSetting("heartbeat_interval_sec", "4"));

        new AlertDialog.Builder(this)
                .setTitle("Heartbeat Interval")
                .setMessage("Enter interval in seconds (2 to 10):")
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> {
                    try {
                        int val = Integer.parseInt(input.getText().toString().trim());
                        if (val >= 2 && val <= 10) {
                            dbHelper.saveSetting("heartbeat_interval_sec", String.valueOf(val));
                            updateTextDisplays();
                            Toast.makeText(this, "Heartbeat interval updated", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Interval must be between 2 and 10 s", Toast.LENGTH_SHORT).show();
                        }
                    } catch (NumberFormatException ignored) {}
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showEditExpiryDialog() {
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setText(dbHelper.getSetting("message_expiry_hours", "24"));

        new AlertDialog.Builder(this)
                .setTitle("Message Expiry")
                .setMessage("Enter expiration duration in hours (1 to 72):")
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> {
                    try {
                        int val = Integer.parseInt(input.getText().toString().trim());
                        if (val >= 1 && val <= 72) {
                            dbHelper.saveSetting("message_expiry_hours", String.valueOf(val));
                            updateTextDisplays();
                            Toast.makeText(this, "Message expiry updated", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Expiry must be between 1 and 72 hours", Toast.LENGTH_SHORT).show();
                        }
                    } catch (NumberFormatException ignored) {}
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void resetSettings() {
        autoDiscoverySwitch.setChecked(true);
        advertisingSwitch.setChecked(true);
        forwardingSwitch.setChecked(true);
        storeForwardSwitch.setChecked(true);
        bootAutoStartSwitch.setChecked(false);
        signMessagesSwitch.setChecked(false);

        dbHelper.saveSetting("max_ttl", "8");
        dbHelper.saveSetting("heartbeat_interval_sec", "4");
        dbHelper.saveSetting("message_expiry_hours", "24");

        String defaultName = android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL;
        meshManager.updateDisplayName(defaultName);
        updateTextDisplays();

        Toast.makeText(this, "Settings reset to defaults", Toast.LENGTH_SHORT).show();
    }
}
