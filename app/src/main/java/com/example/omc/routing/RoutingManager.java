package com.example.omc.routing;

import android.util.Log;

import com.example.omc.mesh.MeshConfig;
import com.example.omc.mesh.MeshNode;

public class RoutingManager {

    private static final String TAG =
            MeshConfig.LOG_ROUTE;

    private final MeshNode localNode;

    private final RoutingTable routingTable;

    public RoutingManager(
            MeshNode localNode
    ) {

        this.localNode = localNode;

        this.routingTable =
                new RoutingTable();
    }

    public void addDirectRoute(
            String peerId
    ) {

        if (peerId == null ||
                peerId.isEmpty()) {

            return;
        }

        Route route =
                new Route(
                        peerId,
                        peerId,
                        1
                );

        routingTable.addRoute(route);

        Log.d(
                TAG,
                "Direct route added: "
                        + peerId
        );
    }

    public void addRoute(
            String destinationId,
            String nextHopId,
            int hopCount
    ) {

        if (destinationId == null ||
                nextHopId == null) {

            return;
        }

        if (destinationId.equals(
                localNode.getNodeId()
        )) {

            return;
        }

        Route existingRoute =
                routingTable.getRoute(
                        destinationId
                );

        /*
         * Prefer the route with fewer hops.
         */
        if (existingRoute != null &&
                existingRoute.getHopCount()
                        <= hopCount) {

            return;
        }

        Route route =
                new Route(
                        destinationId,
                        nextHopId,
                        hopCount
                );

        routingTable.addRoute(route);

        Log.d(
                TAG,
                "Route added: "
                        + destinationId
                        + " via "
                        + nextHopId
        );
    }

    public String findNextHop(
            String destinationId
    ) {

        Route route =
                routingTable.getRoute(
                        destinationId
                );

        if (route == null) {
            return null;
        }

        return route.getNextHopId();
    }

    public Route getRoute(
            String destinationId
    ) {

        return routingTable.getRoute(
                destinationId
        );
    }

    public void removeRoute(
            String destinationId
    ) {

        routingTable.removeRoute(
                destinationId
        );

        Log.d(
                TAG,
                "Route removed: "
                        + destinationId
        );
    }

    public void clearRoutes() {

        routingTable.clear();

        Log.d(
                TAG,
                "Routing table cleared"
        );
    }

    public RoutingTable getRoutingTable() {
        return routingTable;
    }

    public MeshNode getLocalNode() {
        return localNode;
    }
}