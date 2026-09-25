package com.example.omc.discovery;

public class Peer {

    private final String endpointId;
    private String name;
    private String nodeId;
    private boolean connected;
    private long lastSeen;

    public Peer(String endpointId, String name) {
        this.endpointId = endpointId;
        this.name = name;
        this.nodeId = endpointId; // default to endpointId until HELLO exchange
        this.connected = false;
        this.lastSeen = System.currentTimeMillis();
    }

    public String getEndpointId() {
        return endpointId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNodeId() {
        return nodeId != null ? nodeId : endpointId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public long getLastSeen() {
        return lastSeen;
    }

    public void updateLastSeen() {
        this.lastSeen = System.currentTimeMillis();
    }

    @Override
    public String toString() {
        return "Peer{" +
                "endpointId='" + endpointId + '\'' +
                ", name='" + name + '\'' +
                ", nodeId='" + nodeId + '\'' +
                ", connected=" + connected +
                '}';
    }
}