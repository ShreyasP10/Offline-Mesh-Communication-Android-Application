package com.example.omc;

import com.example.omc.mesh.MeshNode;
import com.example.omc.routing.Route;
import com.example.omc.routing.RoutingManager;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class RoutingTest {

    private RoutingManager routingManager;
    private MeshNode localNode;

    @Before
    public void setUp() {
        localNode = new MeshNode("local-node-1", "TestPhone", true);
        routingManager = new RoutingManager(localNode);
    }

    @Test
    public void testDirectRouteAddition() {
        routingManager.addDirectRoute("peer-A");
        Route route = routingManager.getRoute("peer-A");

        assertNotNull(route);
        assertEquals("peer-A", route.getDestinationId());
        assertEquals("peer-A", route.getNextHopId());
        assertEquals(1, route.getHopCount());
    }

    @Test
    public void testMultiHopRoutePreference() {
        // Add 3-hop route
        routingManager.addRoute("dest-D", "relay-B", 3);
        Route route = routingManager.getRoute("dest-D");
        assertEquals(3, route.getHopCount());
        assertEquals("relay-B", routingManager.findNextHop("dest-D"));

        // A shorter 2-hop route appears via relay-C
        routingManager.addRoute("dest-D", "relay-C", 2);
        Route shorterRoute = routingManager.getRoute("dest-D");
        assertEquals(2, shorterRoute.getHopCount());
        assertEquals("relay-C", routingManager.findNextHop("dest-D"));

        // A longer 4-hop route should be ignored
        routingManager.addRoute("dest-D", "relay-E", 4);
        Route finalRoute = routingManager.getRoute("dest-D");
        assertEquals(2, finalRoute.getHopCount());
        assertEquals("relay-C", routingManager.findNextHop("dest-D"));
    }

    @Test
    public void testRouteRemovalAndClear() {
        routingManager.addRoute("dest-X", "relay-Y", 2);
        assertNotNull(routingManager.getRoute("dest-X"));

        routingManager.removeRoute("dest-X");
        assertNull(routingManager.getRoute("dest-X"));

        routingManager.addDirectRoute("peer-1");
        routingManager.addDirectRoute("peer-2");
        assertEquals(2, routingManager.getRoutingTable().size());

        routingManager.clearRoutes();
        assertEquals(0, routingManager.getRoutingTable().size());
    }
}
