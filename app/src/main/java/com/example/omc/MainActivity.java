package com.example.omc;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.omc.discovery.Peer;
import com.example.omc.mesh.MeshManager;
import com.example.omc.service.MeshForegroundService;
import com.example.omc.storage.ChatMessage;
import com.example.omc.ui.ChatActivity;
import com.example.omc.ui.LogsActivity;
import com.example.omc.ui.PeerActivity;
import com.example.omc.ui.PeerAdapter;
import com.example.omc.ui.SettingActivity;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private TextView statusText;
    private TextView meshStatusText;
    private TextView peerCountText;
    private View deviceNameContainer;
    private TextView homeDeviceNameText;
    private View editDeviceNameButton;
    private Button startMeshButton;
    private Button stopMeshButton;
    private RecyclerView peersRecyclerView;

    private Button homeButton;
    private Button chatsButton;
    private Button logsButton;
    private Button settingsButton;

    private PeerAdapter peerAdapter;
    private MeshManager meshManager;

    private ActivityResultLauncher<String[]> permissionLauncher;

    private final MeshManager.MeshListener meshListener = new MeshManager.MeshListener() {
        @Override
        public void onMeshStateChanged(boolean running) {
            updateMeshUI(running);
        }

        @Override
        public void onPeersUpdated(List<Peer> peers) {
            peerAdapter.updatePeers(peers);
            updatePeerCount(peers);
        }

        @Override
        public void onMessageReceived(ChatMessage message) {
            // Live toast or status update
        }

        @Override
        public void onMessageStatusChanged(String messageId, String status) {
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        meshManager = MeshManager.getInstance(this);

        initViews();
        setupPermissionLauncher();
        meshManager.addListener(meshListener);

        updateMeshUI(meshManager.isMeshRunning());
        peerAdapter.updatePeers(meshManager.getDiscoveredPeers());

        // Request permissions on first launch
        checkAndRequestPermissions(false);
    }

    private void initViews() {
        statusText = findViewById(R.id.statusText);
        meshStatusText = findViewById(R.id.meshStatusText);
        peerCountText = findViewById(R.id.peerCountText);
        deviceNameContainer = findViewById(R.id.deviceNameContainer);
        homeDeviceNameText = findViewById(R.id.homeDeviceNameText);
        editDeviceNameButton = findViewById(R.id.editDeviceNameButton);
        startMeshButton = findViewById(R.id.startMeshButton);
        stopMeshButton = findViewById(R.id.stopMeshButton);
        peersRecyclerView = findViewById(R.id.peersRecyclerView);

        homeButton = findViewById(R.id.homeButton);
        chatsButton = findViewById(R.id.chatsButton);
        logsButton = findViewById(R.id.logsButton);
        settingsButton = findViewById(R.id.settingsButton);

        refreshDeviceNameDisplay();

        if (deviceNameContainer != null) {
            deviceNameContainer.setOnClickListener(v -> showChangeDeviceNameDialog());
        }
        if (editDeviceNameButton != null) {
            editDeviceNameButton.setOnClickListener(v -> showChangeDeviceNameDialog());
        }

        peersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        peerAdapter = new PeerAdapter(this::onPeerClicked);
        peersRecyclerView.setAdapter(peerAdapter);

        startMeshButton.setOnClickListener(v -> {
            if (hasAllRequiredPermissions()) {
                if (isRadioReady()) {
                    startMeshService();
                }
            } else {
                checkAndRequestPermissions(true);
            }
        });

        stopMeshButton.setOnClickListener(v -> stopMeshService());

        homeButton.setOnClickListener(v -> {
            // Already home
        });

        chatsButton.setOnClickListener(v -> showChatsChooserDialog());

        logsButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, LogsActivity.class);
            startActivity(intent);
        });

        settingsButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SettingActivity.class);
            startActivity(intent);
        });
    }

    private void setupPermissionLauncher() {
        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    boolean allGranted = true;
                    for (Boolean granted : result.values()) {
                        if (!Boolean.TRUE.equals(granted)) {
                            allGranted = false;
                            break;
                        }
                    }

                    if (allGranted) {
                        Toast.makeText(this, "Permissions granted. Starting mesh...", Toast.LENGTH_SHORT).show();
                        if (isRadioReady()) {
                            startMeshService();
                        }
                    } else {
                        Toast.makeText(this, "Bluetooth, Wi-Fi & Location permissions are required for mesh discovery.", Toast.LENGTH_LONG).show();
                    }
                }
        );
    }

    private boolean isRadioReady() {
        android.bluetooth.BluetoothAdapter btAdapter = android.bluetooth.BluetoothAdapter.getDefaultAdapter();
        if (btAdapter != null && !btAdapter.isEnabled()) {
            Toast.makeText(this, "Please turn ON Bluetooth to discover nearby devices", Toast.LENGTH_LONG).show();
            return false;
        }

        android.location.LocationManager locManager = (android.location.LocationManager) getSystemService(LOCATION_SERVICE);
        if (locManager != null) {
            boolean gps = locManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER);
            boolean net = locManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER);
            if (!gps && !net) {
                Toast.makeText(this, "Please turn ON Location to discover nearby devices", Toast.LENGTH_LONG).show();
                return false;
            }
        }
        return true;
    }

    private boolean hasAllRequiredPermissions() {
        for (String perm : getRequiredPermissions()) {
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private String[] getRequiredPermissions() {
        List<String> list = new ArrayList<>();
        list.add(Manifest.permission.ACCESS_FINE_LOCATION);
        list.add(Manifest.permission.ACCESS_COARSE_LOCATION);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            list.add(Manifest.permission.BLUETOOTH_SCAN);
            list.add(Manifest.permission.BLUETOOTH_ADVERTISE);
            list.add(Manifest.permission.BLUETOOTH_CONNECT);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(Manifest.permission.NEARBY_WIFI_DEVICES);
            list.add(Manifest.permission.POST_NOTIFICATIONS);
        }

        return list.toArray(new String[0]);
    }

    private void checkAndRequestPermissions(boolean startAfterGrant) {
        String[] permissions = getRequiredPermissions();
        List<String> missing = new ArrayList<>();
        for (String p : permissions) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                missing.add(p);
            }
        }

        if (!missing.isEmpty()) {
            permissionLauncher.launch(missing.toArray(new String[0]));
        } else if (startAfterGrant) {
            startMeshService();
        }
    }

    private void startMeshService() {
        MeshForegroundService.start(this);
        updateMeshUI(true);
        Toast.makeText(this, "Mesh Network Started", Toast.LENGTH_SHORT).show();
    }

    private void stopMeshService() {
        MeshForegroundService.stop(this);
        updateMeshUI(false);
        Toast.makeText(this, "Mesh Network Stopped", Toast.LENGTH_SHORT).show();
    }

    private void updateMeshUI(boolean running) {
        if (running) {
            statusText.setText("ONLINE");
            statusText.setBackgroundResource(R.drawable.bg_neu_pill_green);
            statusText.setTextColor(0xFFFFFFFF);
            meshStatusText.setText("Active: " + meshManager.getLocalNode().getNodeName());
            startMeshButton.setEnabled(false);
            stopMeshButton.setEnabled(true);
        } else {
            statusText.setText("OFFLINE");
            statusText.setBackgroundResource(R.drawable.bg_neu_pill_red);
            statusText.setTextColor(0xFFFFFFFF);
            meshStatusText.setText("Ready to discover nearby devices");
            startMeshButton.setEnabled(true);
            stopMeshButton.setEnabled(false);
        }
        updatePeerCount(meshManager.getDiscoveredPeers());
    }

    private void updatePeerCount(List<Peer> peers) {
        int connected = 0;
        int total = peers != null ? peers.size() : 0;
        if (peers != null) {
            for (Peer p : peers) {
                if (p.isConnected()) connected++;
            }
        }
        peerCountText.setText(total + " discovered (" + connected + " connected)");
    }

    private void onPeerClicked(Peer peer) {
        String[] options = {"Open Chat", "View Device Details"};
        new AlertDialog.Builder(this)
                .setTitle(peer.getName())
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        Intent chatIntent = new Intent(MainActivity.this, ChatActivity.class);
                        chatIntent.putExtra(ChatActivity.EXTRA_PEER_ID, peer.getNodeId());
                        chatIntent.putExtra(ChatActivity.EXTRA_PEER_NAME, peer.getName());
                        startActivity(chatIntent);
                    } else {
                        Intent detailIntent = new Intent(MainActivity.this, PeerActivity.class);
                        detailIntent.putExtra(PeerActivity.EXTRA_ENDPOINT_ID, peer.getEndpointId());
                        startActivity(detailIntent);
                    }
                })
                .show();
    }

    private void showChatsChooserDialog() {
        List<Peer> peers = meshManager.getDiscoveredPeers();
        List<String> options = new ArrayList<>();
        options.add("📢 Broadcast Channel (All Nearby Nodes)");

        for (Peer p : peers) {
            options.add("💬 " + p.getName() + (p.isConnected() ? " (Connected)" : " (Offline)"));
        }

        new AlertDialog.Builder(this)
                .setTitle("Select Chat Conversation")
                .setItems(options.toArray(new String[0]), (dialog, which) -> {
                    Intent chatIntent = new Intent(MainActivity.this, ChatActivity.class);
                    if (which == 0) {
                        chatIntent.putExtra(ChatActivity.EXTRA_PEER_ID, ChatMessage.BROADCAST_DESTINATION);
                        chatIntent.putExtra(ChatActivity.EXTRA_PEER_NAME, "Broadcast Channel");
                    } else {
                        Peer selectedPeer = peers.get(which - 1);
                        chatIntent.putExtra(ChatActivity.EXTRA_PEER_ID, selectedPeer.getNodeId());
                        chatIntent.putExtra(ChatActivity.EXTRA_PEER_NAME, selectedPeer.getName());
                    }
                    startActivity(chatIntent);
                })
                .show();
    }

    private void refreshDeviceNameDisplay() {
        if (homeDeviceNameText != null && meshManager != null && meshManager.getLocalNode() != null) {
            homeDeviceNameText.setText(meshManager.getLocalNode().getNodeName());
        }
    }

    private void showChangeDeviceNameDialog() {
        final EditText input = new EditText(this);
        String currentName = meshManager.getLocalNode().getNodeName();
        input.setText(currentName);
        input.setSelection(input.getText().length());
        input.setSingleLine(true);
        input.setPadding(40, 30, 40, 30);

        new AlertDialog.Builder(this)
                .setTitle("Change Phone Name")
                .setMessage("Enter the display name for your offline mesh node. Other nearby phones will detect and chat with you using this name:")
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newName = input.getText().toString().trim();
                    if (!newName.isEmpty()) {
                        meshManager.updateDisplayName(newName);
                        refreshDeviceNameDisplay();
                        updateMeshUI(meshManager.isMeshRunning());
                        Toast.makeText(this, "Phone name set to: " + newName, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Phone name cannot be empty", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshDeviceNameDisplay();
        updateMeshUI(meshManager.isMeshRunning());
        peerAdapter.updatePeers(meshManager.getDiscoveredPeers());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (meshManager != null) {
            meshManager.removeListener(meshListener);
        }
    }
}
