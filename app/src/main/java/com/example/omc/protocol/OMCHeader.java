package com.example.omc.protocol;

public class OMCHeader {

    private String version;
    private String messageId;
    private String sourceId;
    private String destinationId;
    private MessageType messageType;
    private int ttl;
    private int hopCount;
    private long timestamp;
    private String ackForMessageId;

    public OMCHeader() {
        // Required by Gson
    }

    public OMCHeader(
            String version,
            String messageId,
            String sourceId,
            String destinationId,
            MessageType messageType,
            int ttl,
            long timestamp
    ) {
        this.version = version;
        this.messageId = messageId;
        this.sourceId = sourceId;
        this.destinationId = destinationId;
        this.messageType = messageType;
        this.ttl = ttl;
        this.hopCount = 0;
        this.timestamp = timestamp;
    }

    public OMCHeader(
            String version,
            String messageId,
            String sourceId,
            String destinationId,
            MessageType messageType,
            int ttl,
            int hopCount,
            long timestamp,
            String ackForMessageId
    ) {
        this.version = version;
        this.messageId = messageId;
        this.sourceId = sourceId;
        this.destinationId = destinationId;
        this.messageType = messageType;
        this.ttl = ttl;
        this.hopCount = hopCount;
        this.timestamp = timestamp;
        this.ackForMessageId = ackForMessageId;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
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

    public String getDestinationId() {
        return destinationId;
    }

    public void setDestinationId(String destinationId) {
        this.destinationId = destinationId;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }

    public int getTtl() {
        return ttl;
    }

    public void setTtl(int ttl) {
        this.ttl = ttl;
    }

    public int getHopCount() {
        return hopCount;
    }

    public void setHopCount(int hopCount) {
        this.hopCount = hopCount;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getAckForMessageId() {
        return ackForMessageId;
    }

    public void setAckForMessageId(String ackForMessageId) {
        this.ackForMessageId = ackForMessageId;
    }
}