package com.example.omc.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.omc.R;
import com.example.omc.discovery.Peer;
import com.example.omc.mesh.MeshManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PeerActivity extends AppCompatActivity {

    public static final String EXTRA_ENDPOINT_ID = "extra_endpoint_id";

    private String endpointId;
    private MeshManager meshManager;

    private TextView peerName;
    private TextView peerStatus;
    private TextView peerId;
    private TextView peerAddress;
    private TextView peerTransport;
    private TextView peerLastSeen;
    private Button messagePeerButton;
    private ImageButton backButton;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_peer);

        endpointId = getIntent().getStringExtra(EXTRA_ENDPOINT_ID);
        meshManager = MeshManager.getInstance(this);

        initViews();
        displayPeerDetails();
    }

    private void initViews() {
        backButton = findViewById(R.id.backButton);
        peerName = findViewById(R.id.peerName);
        peerStatus = findViewById(R.id.peerStatus);
        peerId = findViewById(R.id.peerId);
        peerAddress = findViewById(R.id.peerAddress);
        peerTransport = findViewById(R.id.peerTransport);
        peerLastSeen = findViewById(R.id.peerLastSeen);
        messagePeerButton = findViewById(R.id.messagePeerButton);

        backButton.setOnClickListener(v -> finish());
    }

    private void displayPeerDetails() {
        Peer peer = null;
        for (Peer p : meshManager.getDiscoveredPeers()) {
            if (p.getEndpointId().equals(endpointId) || p.getNodeId().equals(endpointId)) {
                peer = p;
                break;
            }
        }

        if (peer != null) {
            peerName.setText(peer.getName());
            peerId.setText("Node ID: " + peer.getNodeId());
            peerAddress.setText("Endpoint ID: " + peer.getEndpointId());
            peerTransport.setText("Transport: BLE / Wi-Fi Direct (P2P_CLUSTER)");

            if (peer.isConnected()) {
                peerStatus.setText("Connected");
                peerStatus.setTextColor(0xFF388E3C);
            } else {
                peerStatus.setText("Discovered (Offline / Reconnecting)");
                peerStatus.setTextColor(0xFFF57C00);
            }

            peerLastSeen.setText("Last Seen: " + dateFormat.format(new Date(peer.getLastSeen())));

            final Peer targetPeer = peer;
            messagePeerButton.setOnClickListener(v -> {
                Intent chatIntent = new Intent(PeerActivity.this, ChatActivity.class);
                chatIntent.putExtra(ChatActivity.EXTRA_PEER_ID, targetPeer.getNodeId());
                chatIntent.putExtra(ChatActivity.EXTRA_PEER_NAME, targetPeer.getName());
                startActivity(chatIntent);
            });
        } else {
            peerName.setText("Unknown Peer");
            peerStatus.setText("Disconnected");
            peerId.setText("ID: --");
            peerAddress.setText("Address: " + endpointId);
            peerTransport.setText("Transport: Nearby Connections");
            peerLastSeen.setText("Last Seen: --");
            messagePeerButton.setEnabled(false);
        }
    }
}
