package com.example.omc.discovery;

import android.util.Log;

public class EndpointDiscoveryCallback
        extends com.google.android.gms.nearby.connection.EndpointDiscoveryCallback {

    private static final String TAG = "OMC-MESH";

    public interface Listener {

        void onEndpointFound(
                String endpointId,
                String endpointName
        );

        void onEndpointLost(
                String endpointId
        );
    }

    private final Listener listener;

    public EndpointDiscoveryCallback(Listener listener) {
        this.listener = listener;
    }

    @Override
    public void onEndpointFound(
            String endpointId,
            com.google.android.gms.nearby.connection.DiscoveredEndpointInfo info
    ) {

        Log.d(
                TAG,
                "Endpoint found: "
                        + endpointId
                        + " / "
                        + info.getEndpointName()
        );

        listener.onEndpointFound(
                endpointId,
                info.getEndpointName()
        );
    }

    @Override
    public void onEndpointLost(String endpointId) {

        Log.d(
                TAG,
                "Endpoint lost: " + endpointId
        );

        listener.onEndpointLost(endpointId);
    }
}