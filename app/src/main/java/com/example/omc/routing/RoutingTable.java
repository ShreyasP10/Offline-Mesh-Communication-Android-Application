package com.example.omc.routing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RoutingTable {

    private final Map<String, Route> routes =
            new ConcurrentHashMap<>();

    public void addRoute(Route route) {

        if (route == null) {
            return;
        }

        routes.put(
                route.getDestinationId(),
                route
        );
    }

    public Route getRoute(
            String destinationId
    ) {

        if (destinationId == null) {
            return null;
        }

        return routes.get(destinationId);
    }

    public void removeRoute(
            String destinationId
    ) {

        if (destinationId == null) {
            return;
        }

        routes.remove(destinationId);
    }

    public void clear() {
        routes.clear();
    }

    public boolean containsRoute(
            String destinationId
    ) {

        return routes.containsKey(
                destinationId
        );
    }

    public List<Route> getAllRoutes() {

        return Collections.unmodifiableList(
                new ArrayList<>(routes.values())
        );
    }

    public int size() {
        return routes.size();
    }
}