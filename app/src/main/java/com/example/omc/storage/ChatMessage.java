package com.example.omc.storage;

public class ChatMessage {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_SENT = "SENT";
    public static final String STATUS_DELIVERED = "DELIVERED";
    public static final String STATUS_FAILED = "FAILED";

    public static final String BROADCAST_DESTINATION = "BROADCAST";

    private String messageId;
    private String sourceId;
    private String senderName;
    private String destinationId;
    private String text;
    private String status;
    private long timestamp;
    private boolean isOutgoing;
    private int hopCount;

    public ChatMessage() {
    }

    public ChatMessage(
            String messageId,
            String sourceId,
            String senderName,
            String destinationId,
            String text,
            String status,
            long timestamp,
            boolean isOutgoing,
            int hopCount
    ) {
        this.messageId = messageId;
        this.sourceId = sourceId;
        this.senderName = senderName;
        this.destinationId = destinationId;
        this.text = text;
        this.status = status;
        this.timestamp = timestamp;
        this.isOutgoing = isOutgoing;
        this.hopCount = hopCount;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getSourceId() {
        return sourceId;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getDestinationId() {
        return destinationId;
    }

    public void setDestinationId(String destinationId) {
        this.destinationId = destinationId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isOutgoing() {
        return isOutgoing;
    }

    public void setOutgoing(boolean outgoing) {
        isOutgoing = outgoing;
    }

    public int getHopCount() {
        return hopCount;
    }

    public void setHopCount(int hopCount) {
        this.hopCount = hopCount;
    }

    public boolean isBroadcast() {
        return BROADCAST_DESTINATION.equalsIgnoreCase(destinationId);
    }
}
