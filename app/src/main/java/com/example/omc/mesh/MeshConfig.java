package com.example.omc.mesh;

public final class MeshConfig {

    private MeshConfig() {
        // Utility class
    }

    public static final String SERVICE_ID = "com.example.omc.mesh";

    public static final String STRATEGY = "P2P_CLUSTER";

    public static final int DEFAULT_TTL = 8;

    public static final long PEER_TIMEOUT_MS = 30_000L;

    public static final long HEARTBEAT_INTERVAL_MS = 10_000L;


    public static final String PROTOCOL_VERSION = "OMC/1.0";


    public static final int MAX_MESSAGE_SIZE = 32 * 1024;

    public static final String LOG_MESH = "OMC-MESH";
    public static final String LOG_ROUTE = "OMC-ROUTE";
    public static final String LOG_DTN = "OMC-DTN";
    public static final String LOG_ACK = "OMC-ACK";
    public static final String LOG_HEARTBEAT = "OMC-HB";
}