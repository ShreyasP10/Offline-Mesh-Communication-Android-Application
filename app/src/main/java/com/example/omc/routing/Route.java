package com.example.omc.routing;

public class Route {

    private final String destinationId;
    private final String nextHopId;

    private final int hopCount;

    private long lastUpdated;

    public Route(
            String destinationId,
            String nextHopId,
            int hopCount
    ) {

        this.destinationId = destinationId;
        this.nextHopId = nextHopId;
        this.hopCount = hopCount;

        this.lastUpdated =
                System.currentTimeMillis();
    }

    public String getDestinationId() {
        return destinationId;
    }

    public String getNextHopId() {
        return nextHopId;
    }

    public int getHopCount() {
        return hopCount;
    }

    public long getLastUpdated() {
        return lastUpdated;
    }

    public void refresh() {
        lastUpdated =
                System.currentTimeMillis();
    }

    @Override
    public String toString() {

        return "Route{" +
                "destinationId='" + destinationId + '\'' +
                ", nextHopId='" + nextHopId + '\'' +
                ", hopCount=" + hopCount +
                '}';
    }
}