package com.example.omc.discovery;

public class Peer {

    private final String endpointId;
    private final String name;

    private boolean connected;

    private long lastSeen;

    public Peer(String endpointId, String name) {

        this.endpointId = endpointId;
        this.name = name;

        this.connected = false;
        this.lastSeen = System.currentTimeMillis();
    }

    public String getEndpointId() {
        return endpointId;
    }

    public String getName() {
        return name;
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
                ", connected=" + connected +
                '}';
    }
}