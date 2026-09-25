package com.example.omc.ui;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.omc.R;
import com.example.omc.mesh.MeshManager;
import com.example.omc.storage.DatabaseHelper;

public class SettingActivity extends AppCompatActivity {

    private Switch autoDiscoverySwitch;
    private Switch advertisingSwitch;
    private Switch forwardingSwitch;
    private Switch storeForwardSwitch;
    private TextView deviceNameText;
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
        autoDiscoverySwitch = findViewById(R.id.autoDiscoverySwitch);
        advertisingSwitch = findViewById(R.id.advertisingSwitch);
        forwardingSwitch = findViewById(R.id.forwardingSwitch);
        storeForwardSwitch = findViewById(R.id.storeForwardSwitch);
        deviceNameText = findViewById(R.id.deviceNameText);
        resetSettingsButton = findViewById(R.id.resetSettingsButton);

        deviceNameText.setOnClickListener(v -> showEditDeviceNameDialog());

        autoDiscoverySwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
                dbHelper.saveSetting("auto_discovery", String.valueOf(isChecked)));

        advertisingSwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
                dbHelper.saveSetting("advertising", String.valueOf(isChecked)));

        forwardingSwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
                dbHelper.saveSetting("forwarding", String.valueOf(isChecked)));

        storeForwardSwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
                dbHelper.saveSetting("store_forward", String.valueOf(isChecked)));

        resetSettingsButton.setOnClickListener(v -> resetSettings());
    }

    private void loadSettings() {
        boolean autoDiscovery = Boolean.parseBoolean(dbHelper.getSetting("auto_discovery", "true"));
        boolean advertising = Boolean.parseBoolean(dbHelper.getSetting("advertising", "true"));
        boolean forwarding = Boolean.parseBoolean(dbHelper.getSetting("forwarding", "true"));
        boolean storeForward = Boolean.parseBoolean(dbHelper.getSetting("store_forward", "true"));

        autoDiscoverySwitch.setChecked(autoDiscovery);
        advertisingSwitch.setChecked(advertising);
        forwardingSwitch.setChecked(forwarding);
        storeForwardSwitch.setChecked(storeForward);

        updateDeviceNameDisplay();
    }

    private void updateDeviceNameDisplay() {
        String name = meshManager.getLocalNode().getNodeName();
        deviceNameText.setText("Device Name: " + name + " (Tap to change)");
    }

    private void showEditDeviceNameDialog() {
        final EditText input = new EditText(this);
        input.setText(meshManager.getLocalNode().getNodeName());
        input.setSelection(input.getText().length());

        new AlertDialog.Builder(this)
                .setTitle("Change Device Name")
                .setMessage("Enter the display name for your mesh node:")
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newName = input.getText().toString().trim();
                    if (!newName.isEmpty()) {
                        meshManager.updateDisplayName(newName);
                        updateDeviceNameDisplay();
                        Toast.makeText(this, "Device name updated", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void resetSettings() {
        autoDiscoverySwitch.setChecked(true);
        advertisingSwitch.setChecked(true);
        forwardingSwitch.setChecked(true);
        storeForwardSwitch.setChecked(true);

        String defaultName = android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL;
        meshManager.updateDisplayName(defaultName);
        updateDeviceNameDisplay();

        Toast.makeText(this, "Settings reset to default", Toast.LENGTH_SHORT).show();
    }
}
