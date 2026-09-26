package com.example.omc.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.omc.R;
import com.example.omc.storage.ChatMessage;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;

    public interface OnMessageLongClickListener {
        void onMessageLongClick(ChatMessage message);
    }

    private final List<ChatMessage> messages = new ArrayList<>();
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private OnMessageLongClickListener longClickListener;

    public MessageAdapter(OnMessageLongClickListener longClickListener) {
        this.longClickListener = longClickListener;
    }

    public void setMessages(List<ChatMessage> newMessages) {
        messages.clear();
        if (newMessages != null) {
            messages.addAll(newMessages);
        }
        notifyDataSetChanged();
    }

    public void addMessage(ChatMessage message) {
        if (message != null) {
            messages.add(message);
            notifyItemInserted(messages.size() - 1);
        }
    }

    public void updateMessageStatus(String messageId, String status) {
        for (int i = 0; i < messages.size(); i++) {
            ChatMessage msg = messages.get(i);
            if (msg.getMessageId().equals(messageId)) {
                msg.setStatus(status);
                notifyItemChanged(i);
                break;
            }
        }
    }

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).isOutgoing() ? VIEW_TYPE_SENT : VIEW_TYPE_RECEIVED;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_SENT) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_sent, parent, false);
            return new SentViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_received, parent, false);
            return new ReceivedViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = messages.get(position);
        if (holder instanceof SentViewHolder) {
            ((SentViewHolder) holder).bind(message, timeFormat, longClickListener);
        } else if (holder instanceof ReceivedViewHolder) {
            ((ReceivedViewHolder) holder).bind(message, timeFormat);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class SentViewHolder extends RecyclerView.ViewHolder {
        private final TextView messageText;
        private final TextView messageTime;

        SentViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.messageText);
            messageTime = itemView.findViewById(R.id.messageTime);
        }

        void bind(ChatMessage message, SimpleDateFormat format, OnMessageLongClickListener listener) {
            messageText.setText(message.getText());

            String statusTick = "";
            if (ChatMessage.STATUS_PENDING.equals(message.getStatus())) {
                statusTick = " ⏳";
            } else if (ChatMessage.STATUS_SENT.equals(message.getStatus())) {
                statusTick = " ➤";
            } else if (ChatMessage.STATUS_DELIVERED.equals(message.getStatus())) {
                statusTick = " ✓";
            } else if (ChatMessage.STATUS_FAILED.equals(message.getStatus())) {
                statusTick = " ✗ (tap to retry)";
            }

            String timeStr = format.format(new Date(message.getTimestamp())) + statusTick;
            messageTime.setText(timeStr);

            itemView.setOnLongClickListener(v -> {
                if (listener != null) {
                    listener.onMessageLongClick(message);
                    return true;
                }
                return false;
            });

            if (ChatMessage.STATUS_FAILED.equals(message.getStatus())) {
                itemView.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onMessageLongClick(message);
                    }
                });
            } else {
                itemView.setOnClickListener(null);
            }
        }
    }

    static class ReceivedViewHolder extends RecyclerView.ViewHolder {
        private final TextView messageText;
        private final TextView messageTime;

        ReceivedViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.messageText);
            messageTime = itemView.findViewById(R.id.messageTime);
        }

        void bind(ChatMessage message, SimpleDateFormat format) {
            messageText.setText(message.getText());
            String senderInfo = (message.getSenderName() != null ? message.getSenderName() : "Peer")
                    + " · " + format.format(new Date(message.getTimestamp()));
            if (message.getHopCount() > 0) {
                senderInfo += " (via " + message.getHopCount() + " hops)";
            }
            messageTime.setText(senderInfo);
        }
    }
}
