package com.example.omc.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.omc.R;
import com.example.omc.discovery.Peer;
import com.example.omc.mesh.MeshManager;
import com.example.omc.storage.ChatMessage;
import com.example.omc.storage.DatabaseHelper;

import java.util.List;

public class ChatActivity extends AppCompatActivity {

    public static final String EXTRA_PEER_ID = "extra_peer_id";
    public static final String EXTRA_PEER_NAME = "extra_peer_name";

    private String peerId;
    private String peerName;

    private TextView chatPeerName;
    private TextView chatPeerStatus;
    private RecyclerView messagesRecyclerView;
    private EditText messageInput;
    private ImageButton sendButton;
    private ImageButton backButton;

    private MessageAdapter messageAdapter;
    private MeshManager meshManager;
    private DatabaseHelper dbHelper;

    private final MeshManager.MeshListener meshListener = new MeshManager.MeshListener() {
        @Override
        public void onMeshStateChanged(boolean running) {
            updatePeerStatus();
        }

        @Override
        public void onPeersUpdated(List<Peer> peers) {
            updatePeerStatus();
        }

        @Override
        public void onMessageReceived(ChatMessage message) {
            if (message != null) {
                // If message belongs to this conversation
                if (peerId.equals(message.getSourceId()) ||
                        peerId.equals(message.getDestinationId()) ||
                        ChatMessage.BROADCAST_DESTINATION.equalsIgnoreCase(peerId)) {
                    messageAdapter.addMessage(message);
                    messagesRecyclerView.smoothScrollToPosition(messageAdapter.getItemCount() - 1);
                }
            }
        }

        @Override
        public void onMessageStatusChanged(String messageId, String status) {
            messageAdapter.updateMessageStatus(messageId, status);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        peerId = getIntent().getStringExtra(EXTRA_PEER_ID);
        peerName = getIntent().getStringExtra(EXTRA_PEER_NAME);

        if (peerId == null) {
            peerId = ChatMessage.BROADCAST_DESTINATION;
        }
        if (peerName == null) {
            peerName = ChatMessage.BROADCAST_DESTINATION.equalsIgnoreCase(peerId) ? "Broadcast Channel" : "Peer Device";
        }

        meshManager = MeshManager.getInstance(this);
        dbHelper = DatabaseHelper.getInstance(this);

        initViews();
        loadMessages();
        updatePeerStatus();

        meshManager.addListener(meshListener);
    }

    private void initViews() {
        chatPeerName = findViewById(R.id.chatPeerName);
        chatPeerStatus = findViewById(R.id.chatPeerStatus);
        messagesRecyclerView = findViewById(R.id.messagesRecyclerView);
        messageInput = findViewById(R.id.messageInput);
        sendButton = findViewById(R.id.sendButton);
        backButton = findViewById(R.id.backButton);

        chatPeerName.setText(peerName);

        backButton.setOnClickListener(v -> finish());

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        messagesRecyclerView.setLayoutManager(layoutManager);

        messageAdapter = new MessageAdapter(this::showRetryDialog);
        messagesRecyclerView.setAdapter(messageAdapter);

        sendButton.setOnClickListener(v -> sendMessage());
    }

    private void loadMessages() {
        List<ChatMessage> conversation = dbHelper.getMessagesForConversation(
                peerId,
                meshManager.getLocalNode().getNodeId()
        );
        messageAdapter.setMessages(conversation);
        if (messageAdapter.getItemCount() > 0) {
            messagesRecyclerView.scrollToPosition(messageAdapter.getItemCount() - 1);
        }
    }

    private void updatePeerStatus() {
        if (ChatMessage.BROADCAST_DESTINATION.equalsIgnoreCase(peerId)) {
            int activePeers = meshManager.getConnectedPeerCount();
            chatPeerStatus.setText("Broadcast to all (" + activePeers + " reachable)");
            return;
        }

        Peer peer = meshManager.findPeerByNodeId(peerId);
        if (peer != null && peer.isConnected()) {
            chatPeerStatus.setText("Connected");
        } else {
            chatPeerStatus.setText("Offline / Relay (Store & Forward enabled)");
        }
    }

    private void sendMessage() {
        String text = messageInput.getText().toString().trim();
        if (TextUtils.isEmpty(text)) {
            return;
        }

        messageInput.setText("");
        String msgId = meshManager.sendChatMessage(peerId, text);
        if (msgId == null) {
            Toast.makeText(this, "Failed to build packet", Toast.LENGTH_SHORT).show();
        }
    }

    private void showRetryDialog(ChatMessage message) {
        if (ChatMessage.STATUS_FAILED.equals(message.getStatus())) {
            new AlertDialog.Builder(this)
                    .setTitle("Retry Message")
                    .setMessage("Would you like to resend this message via the mesh?")
                    .setPositiveButton("Retry", (dialog, which) -> {
                        meshManager.retryMessage(message.getMessageId());
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (meshManager != null) {
            meshManager.removeListener(meshListener);
        }
    }
}
