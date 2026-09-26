package com.example.omc.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.omc.R;
import com.example.omc.discovery.Peer;

import java.util.ArrayList;
import java.util.List;

public class PeerAdapter extends RecyclerView.Adapter<PeerAdapter.PeerViewHolder> {

    public interface OnPeerClickListener {
        void onPeerClick(Peer peer);
    }

    private final List<Peer> peers = new ArrayList<>();
    private final OnPeerClickListener listener;

    public PeerAdapter(OnPeerClickListener listener) {
        this.listener = listener;
    }

    public void updatePeers(List<Peer> newPeers) {
        peers.clear();
        if (newPeers != null) {
            peers.addAll(newPeers);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PeerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_peer, parent, false);
        return new PeerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PeerViewHolder holder, int position) {
        Peer peer = peers.get(position);
        holder.bind(peer, listener);
    }

    @Override
    public int getItemCount() {
        return peers.size();
    }

    static class PeerViewHolder extends RecyclerView.ViewHolder {
        private final TextView peerName;
        private final TextView peerId;
        private final TextView peerStatus;
        private final ImageView connectionIndicator;

        PeerViewHolder(@NonNull View itemView) {
            super(itemView);
            peerName = itemView.findViewById(R.id.peerName);
            peerId = itemView.findViewById(R.id.peerId);
            peerStatus = itemView.findViewById(R.id.peerStatus);
            connectionIndicator = itemView.findViewById(R.id.connectionIndicator);
        }

        void bind(final Peer peer, final OnPeerClickListener listener) {
            peerName.setText(peer.getName());
            peerId.setText("ID: " + peer.getNodeId());

            if (peer.isConnected()) {
                peerStatus.setText("Connected");
                peerStatus.setTextColor(0xFF10B981); // neu_accent_green
                connectionIndicator.setImageResource(android.R.drawable.presence_online);
            } else {
                peerStatus.setText("Discovered (Connecting...)");
                peerStatus.setTextColor(0xFFF59E0B); // neu_accent_orange
                connectionIndicator.setImageResource(android.R.drawable.presence_away);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPeerClick(peer);
                }
            });
        }
    }
}
